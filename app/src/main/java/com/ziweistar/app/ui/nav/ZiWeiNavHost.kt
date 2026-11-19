package com.ziweistar.app.ui.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ziweistar.app.engine.ReportEngine
import com.ziweistar.app.ui.MainViewModel
import com.ziweistar.app.ui.screens.ArchiveScreen
import com.ziweistar.app.ui.screens.BossKeyScreen
import com.ziweistar.app.ui.screens.CalendarScreen
import com.ziweistar.app.ui.screens.GlossaryScreen
import com.ziweistar.app.ui.screens.HomeScreen
import com.ziweistar.app.ui.screens.ImmersionScreen
import com.ziweistar.app.ui.screens.MeikaiScreen
import com.ziweistar.app.ui.screens.NatalChartScreen
import com.ziweistar.app.ui.screens.PactScreen
import com.ziweistar.app.ui.screens.PhysiqueScreen
import com.ziweistar.app.ui.screens.ReportScreen
import com.ziweistar.app.ui.screens.SettingsScreen
import com.ziweistar.app.ui.screens.SplashScreen
import com.ziweistar.app.ui.screens.TimerScreen
import com.ziweistar.app.ui.security.BiometricGate

/**
 * 全局导航图。
 *
 * 归档抽屉与梅开二度贺卡以 overlay 形式叠加在 NavHost 之上（不占路由栈），
 * 这样「收功」后的抽屉可直接压在计时页之上，背后星轨仍然可见。
 */
@Composable
fun ZiWeiNavHost(
    bossKey: Boolean,
    onBossKeyExit: () -> Unit,
) {
    val vm: MainViewModel = viewModel()

    if (bossKey) {
        BossKeyScreen(onExit = onBossKeyExit)
        return
    }

    val nav = rememberNavController()

    // 归档 / 梅开二度 overlay 状态
    var archiveArgs by remember {
        mutableStateOf<Triple<Long, Long, Pair<Int, Int>>?>(null)
    }
    var showMeikai by remember { mutableStateOf(false) }
    var showImmersion by remember { mutableStateOf(false) }

    val homeState by vm.home.collectAsStateWithLifecycle()
    val onboarded = remember(homeState) { homeState?.profile?.onboardingDone == true }

    // ---------- 天机加锁：生物识别门禁（设计文档 7） ----------
    // 每次进程启动验证一次；未开启 / 设备无认证方式时 BiometricGate 内部直接放行。
    var gatePassed by remember { mutableStateOf(false) }
    val biometricOn = homeState?.profile?.biometricLock == true
    if (homeState != null && biometricOn && !gatePassed) {
        BiometricGate(
            enabled = true,
            onPass = { gatePassed = true },
        )
        return
    }

    NavHost(
        navController = nav,
        startDestination = Routes.SPLASH,
        enterTransition = {
            fadeIn(tween(260)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(280),
            )
        },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = {
            fadeOut(tween(200)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(260),
            )
        },
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                // 注意：必须读取 StateFlow 的当前值，不能用外面捕获的 onboarded 布尔量。
                // LaunchedEffect(Unit) 只在首帧跑一次，那时数据库尚未返回、onboarded 恒为 false，
                // 会造成「重启后仍进立命盘」的致命回归。
                hasProfile = { vm.home.value?.profile?.onboardingDone == true },
                isReady = { vm.home.value != null },
                onFinished = {
                    val target = if (vm.home.value?.profile?.onboardingDone == true) {
                        Routes.HOME
                    } else {
                        Routes.ONBOARD_NATAL
                    }
                    nav.navigate(target) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        // ---------- 立命盘三步 ----------
        composable(Routes.ONBOARD_NATAL) {
            NatalChartScreen(vm = vm, onNext = { nav.navigate(Routes.ONBOARD_PACT) })
        }
        composable(Routes.ONBOARD_PACT) {
            PactScreen(
                vm = vm,
                onAgreed = { nav.navigate(Routes.ONBOARD_PHYSIQUE) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.ONBOARD_PHYSIQUE) {
            PhysiqueScreen(
                vm = vm,
                onFinish = {
                    nav.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARD_NATAL) { inclusive = true }
                    }
                },
            )
        }

        // ---------- 首页 ----------
        composable(Routes.HOME) {
            HomeScreen(
                vm = vm,
                onStartSession = {
                    vm.beginSession()
                    nav.navigate(Routes.TIMER)
                },
                onOpenCalendar = { nav.navigate(Routes.CALENDAR) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenGlossary = { nav.navigate(Routes.GLOSSARY) },
            )
        }

        // ---------- 起坛计时 ----------
        composable(Routes.TIMER) {
            TimerScreen(
                vm = vm,
                onFinish = { startedAt, endedAt, durationSec, pausedSec ->
                    vm.resetArchive()
                    archiveArgs = Triple(startedAt, endedAt, durationSec to pausedSec)
                },
                onBack = {
                    vm.clearTimer()
                    nav.popBackStack()
                },
            )
        }

        // ---------- 星历 ----------
        composable(Routes.CALENDAR) {
            CalendarScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onOpenReport = { kind ->
                    nav.navigate(Routes.REPORT_WITH_ARG.replace("{periodKey}", kind.name))
                },
            )
        }

        // ---------- 内丹录 ----------
        composable(
            route = Routes.REPORT_WITH_ARG,
            arguments = listOf(navArgument(Routes.REPORT_ARG) { type = NavType.StringType }),
        ) { entry ->
            val raw = entry.arguments?.getString(Routes.REPORT_ARG).orEmpty()
            val kind = runCatching { ReportEngine.Kind.valueOf(raw) }
                .getOrDefault(ReportEngine.Kind.WEEK)
            ReportScreen(vm = vm, initialKind = kind, onBack = { nav.popBackStack() })
        }

        // ---------- 天机阁 ----------
        composable(Routes.SETTINGS) {
            SettingsScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onOpenGlossary = { nav.navigate(Routes.GLOSSARY) },
                onBurnt = {
                    nav.navigate(Routes.ONBOARD_NATAL) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        // ---------- 入定（收功后十五分钟温养，设计文档 3.3） ----------
        composable(Routes.IMMERSION) {
            val qi = vm.home.value?.qi ?: 100
            ImmersionScreen(
                qi = qi,
                onDismiss = {
                    nav.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }

        // ---------- 玄门释义 ----------
        composable(Routes.GLOSSARY) {
            GlossaryScreen(onBack = { nav.popBackStack() })
        }
    }

    // ---------- 收功抽屉（覆盖在计时页之上） ----------
    val args = archiveArgs
    if (args != null) {
        val (startedAt, endedAt, dur) = args
        ArchiveScreen(
            vm = vm,
            startedAt = startedAt,
            endedAt = endedAt,
            durationSeconds = dur.first,
            pausedSeconds = dur.second,
            onSaved = { result ->
                archiveArgs = null
                vm.clearTimer()
                // 回到首页
                nav.navigate(Routes.HOME) {
                    popUpTo(Routes.HOME) { inclusive = true }
                }
                if (result.meikai) {
                    showMeikai = true
                }
                // 设计文档 3.3：收功后进入 15 分钟「入定」温养
                // 未触犯天罡（无严重不良反应）才引导入定；已越界时让天罡黄符先行拦截
                if (!result.tianggang) {
                    showImmersion = true
                }
            },
            onCancel = {
                archiveArgs = null
                vm.clearTimer()
                nav.popBackStack()
            },
        )
    }

    // ---------- 梅开二度烫金贺卡 ----------
    if (showMeikai) {
        val meikaiDoc by vm.meikaiDoc.collectAsStateWithLifecycle()
        MeikaiScreen(
            onDismiss = { showMeikai = false },
            doc = meikaiDoc,
            onNeedDoc = { vm.loadMeikaiDoc() },
        )
    }

    // ---------- 入定（收功后十五分钟温养，设计文档 3.3） ----------
    // 与梅开二度贺卡可能同时触发：贺卡优先级更高（喜报先报，再入定温养）。
    // 若两个 overlay 同时绘制，后绘制的入定页会盖住贺卡——所以贺卡在时先不挂入定。
    if (showImmersion && !showMeikai) {
        ImmersionScreen(
            qi = vm.home.value?.qi ?: 100,
            onDismiss = {
                showImmersion = false
                // 出定后若已触发天罡戒律，让黄符接棒劝导（HomeScreen 内部自行判定 sealed）
            },
        )
    }
}

