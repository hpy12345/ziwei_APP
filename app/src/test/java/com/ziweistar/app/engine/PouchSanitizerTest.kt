package com.ziweistar.app.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 锦囊正文清洗器测试（用户要求：锦囊中不出现个人身体信息）
 *
 * 用例取自 assets/oracle_templates*.json 里真实模板经占位符替换后的原文，
 * 目的是保证清洗后既没有体征信息，也不会留下「本不耐久持」这类缺主语的残句。
 */
class PouchSanitizerTest {

    @Test
    fun `不含身体信息的正文原样返回`() {
        val note = "戌时阴气渐生，宜早卧以养元神。素榻清枕，熄烛而眠。"
        assertTrue(note == PouchSanitizer.clean(note))
    }

    @Test
    fun `含骨相与肉身的短句被剥离`() {
        val note = "徒手宗不借外物，全凭指掌分寸，正合中正之骨相；阁下雄伟型，故宜由浅入深，徐徐试探。"
        val out = PouchSanitizer.clean(note)
        assertFalse("实际输出: $out", out.contains("骨相"))
        assertFalse("实际输出: $out", out.contains("肉身"))
        assertFalse("实际输出: $out", out.contains("雄伟型"))
        // 非身体信息的短句应保留
        assertTrue("实际输出: $out", out.contains("徒手宗不借外物"))
        assertTrue("实际输出: $out", out.contains("徐徐试探"))
    }

    @Test
    fun `真实模板_徒手宗贵在分寸_清洗后不留残缺句`() {
        val note = "徒手宗贵在分寸，力轻则气行，力重则气滞。" +
            "阁下匀停骨相，本不耐久持；" +
            "乾造·匀停型·雄伟型，故指尖一点即止，最合今日浅尝之数。若贪其尽，则如竭泽。"
        val out = PouchSanitizer.clean(note)
        assertFalse("实际输出: $out", out.contains("骨相"))
        assertFalse("实际输出: $out", out.contains("匀停型"))
        // 承接被删主语后的「本不耐久持」属于残句，应一并丢弃
        assertFalse("实际输出: $out", out.contains("本不耐久持"))
        assertTrue("实际输出: $out", out.contains("徒手宗贵在分寸"))
        assertTrue("实际输出: $out", out.contains("若贪其尽"))
    }

    @Test
    fun `真实模板_洞府注_清洗后保留结论`() {
        val note = "净室水汽轻薄，星宿之气只浮于池面而不下沉，" +
            "与阁下骨相之清者最相宜，" +
            "故此处只宜小坐片刻，不宜久浸其中。"
        val out = PouchSanitizer.clean(note)
        assertFalse("实际输出: $out", out.contains("骨相"))
        assertTrue("实际输出: $out", out.contains("净室水汽轻薄"))
        assertTrue("实际输出: $out", out.contains("不宜久浸其中"))
    }

    @Test
    fun `真实大凶模板_清洗后保留动作指引`() {
        val note = "观阁下匀停骨相之质，乾造·匀停型·雄伟型。今日不宜再耗，宜盘膝吐纳、敛息静心，使气血归元。"
        val out = PouchSanitizer.clean(note)
        assertFalse("实际输出: $out", out.contains("骨相"))
        assertTrue("实际输出: $out", out.contains("今日不宜再耗"))
        assertTrue("实际输出: $out", out.contains("敛息静心"))
    }

    @Test
    fun `连续两句身体信息只保留其余内容`() {
        val note = "阁下骨相中正，指法自合经脉；肉身雄伟型，耐得缓引。故徒手一脉最契本心，徐而不迫，力自内生。"
        val out = PouchSanitizer.clean(note)
        assertFalse("实际输出: $out", out.contains("骨相"))
        assertFalse("实际输出: $out", out.contains("肉身"))
        assertTrue("实际输出: $out", out.contains("故徒手一脉最契本心"))
        assertTrue("实际输出: $out", out.endsWith("。"))
    }

    @Test
    fun `残留占位符也会被识别并剥离`() {
        val note = "观阁下{{bone}}之质，{{physique}}。今日不宜再耗，宜盘膝吐纳。"
        val out = PouchSanitizer.clean(note)
        assertFalse("实际输出: $out", out.contains("{{bone}}"))
        assertFalse("实际输出: $out", out.contains("{{physique}}"))
        assertTrue("实际输出: $out", out.contains("宜盘膝吐纳"))
    }

    @Test
    fun `尺寸与尺码类数值也被剥离`() {
        val note = "今夜宜缓，长度 14.0cm 周长 12.0cm 者尤须留意；气到即收，不必尽兴。"
        val out = PouchSanitizer.clean(note)
        assertFalse("实际输出: $out", out.contains("cm"))
        assertFalse("实际输出: $out", out.contains("长度"))
        assertTrue("实际输出: $out", out.contains("不必尽兴"))
    }

    @Test
    fun `全部为身体信息时不会返回空串`() {
        val note = "阁下骨相中正，肉身雄伟型。"
        val out = PouchSanitizer.clean(note)
        // 清洗后若为空则回退原文，避免 UI 出现空段
        assertTrue(out.isNotEmpty())
    }

    @Test
    fun `清洗结果不含悬空标点`() {
        val note = "净室之水属阴而含阳，与阁下骨相中一点灵光暗合，故此刻入水最易感应。"
        val out = PouchSanitizer.clean(note)
        assertFalse("实际输出: $out", out.startsWith("，"))
        assertFalse("实际输出: $out", out.contains("，，"))
        assertFalse("实际输出: $out", out.contains("，。"))
    }
}
