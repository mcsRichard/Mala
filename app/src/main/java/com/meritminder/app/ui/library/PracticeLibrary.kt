package com.meritminder.app.ui.library

object PracticeLibrary {

    data class Template(val name: String, val subtitle: String)

    val mantras = listOf(
        Template("金刚萨埵心咒", "六字/百字明"),
        Template("莲师心咒", "嗡阿吽班扎格鲁"),
        Template("观音心咒", "六字大明咒"),
        Template("文殊心咒", "嗡阿Ra巴扎那娑"),
        Template("度母心咒", "绿度母/白度母"),
        Template("普巴金刚心咒", "忿怒除障本尊"),
        Template("药师佛心咒", "增福除病"),
        Template("阿弥陀佛心咒", "往生净土"),
        Template("长寿佛心咒", "无量寿佛"),
        Template("财神心咒", "黄财神/五姓财神"),
        Template("金刚亥母心咒", "空行智慧母"),
        Template("马头明王心咒", "忿怒除魔障")
    )

    val sutras = listOf(
        Template("金刚经", "空性智慧"),
        Template("心经", "般若波罗蜜多"),
        Template("般若摄颂", "般若波罗蜜多"),
        Template("地藏菩萨本愿经", "净障消业"),
        Template("普门品", "观音感应品"),
        Template("长寿经", "增寿增福"),
        Template("阿弥陀经", "净土往生"),
        Template("药师经", "消灾延寿"),
        Template("大乘离文字经", "普光明藏"),
        Template("优陀那经", ""),
        Template("入中论", ""),
        Template("前行广释", ""),
        Template("西游回忆录", ""),
        Template("水月会一轮", "")
    )

    val ngondro = listOf(
        Template("顶礼", "大礼拜·十万次"),
        Template("皈依", "皈依发心·十万遍"),
        Template("发心", "菩提心·十万遍"),
        Template("百字明", "金刚萨埵·十万遍"),
        Template("供曼扎", "积资·十万次"),
        Template("莲师上师瑜伽", "祈请莲师·十万次")
    )

    val prayers = listOf(
        Template("上师瑜伽", "法主如意宝等"),
        Template("三十五佛忏悔文", "净障忏悔"),
        Template("七支供养", "积资净障"),
        Template("意乐任运自成", "祈祷文"),
        Template("心经回遮", "迴遮连续"),
        Template("供护法", "护法食子"),
        Template("喇嘛钦", "莲师祈祷文"),
        Template("二十一度母礼赞", "顶礼文")
    )
}
