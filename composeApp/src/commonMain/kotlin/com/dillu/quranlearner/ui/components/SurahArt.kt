package com.dillu.quranlearner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.painterResource
import quranlearner.composeapp.generated.resources.Res
import quranlearner.composeapp.generated.resources.surah_name_1
import quranlearner.composeapp.generated.resources.surah_name_10
import quranlearner.composeapp.generated.resources.surah_name_100
import quranlearner.composeapp.generated.resources.surah_name_101
import quranlearner.composeapp.generated.resources.surah_name_102
import quranlearner.composeapp.generated.resources.surah_name_103
import quranlearner.composeapp.generated.resources.surah_name_104
import quranlearner.composeapp.generated.resources.surah_name_105
import quranlearner.composeapp.generated.resources.surah_name_106
import quranlearner.composeapp.generated.resources.surah_name_107
import quranlearner.composeapp.generated.resources.surah_name_108
import quranlearner.composeapp.generated.resources.surah_name_109
import quranlearner.composeapp.generated.resources.surah_name_11
import quranlearner.composeapp.generated.resources.surah_name_110
import quranlearner.composeapp.generated.resources.surah_name_111
import quranlearner.composeapp.generated.resources.surah_name_112
import quranlearner.composeapp.generated.resources.surah_name_113
import quranlearner.composeapp.generated.resources.surah_name_114
import quranlearner.composeapp.generated.resources.surah_name_12
import quranlearner.composeapp.generated.resources.surah_name_13
import quranlearner.composeapp.generated.resources.surah_name_14
import quranlearner.composeapp.generated.resources.surah_name_15
import quranlearner.composeapp.generated.resources.surah_name_16
import quranlearner.composeapp.generated.resources.surah_name_17
import quranlearner.composeapp.generated.resources.surah_name_18
import quranlearner.composeapp.generated.resources.surah_name_19
import quranlearner.composeapp.generated.resources.surah_name_2
import quranlearner.composeapp.generated.resources.surah_name_20
import quranlearner.composeapp.generated.resources.surah_name_21
import quranlearner.composeapp.generated.resources.surah_name_22
import quranlearner.composeapp.generated.resources.surah_name_23
import quranlearner.composeapp.generated.resources.surah_name_24
import quranlearner.composeapp.generated.resources.surah_name_25
import quranlearner.composeapp.generated.resources.surah_name_26
import quranlearner.composeapp.generated.resources.surah_name_27
import quranlearner.composeapp.generated.resources.surah_name_28
import quranlearner.composeapp.generated.resources.surah_name_29
import quranlearner.composeapp.generated.resources.surah_name_3
import quranlearner.composeapp.generated.resources.surah_name_30
import quranlearner.composeapp.generated.resources.surah_name_31
import quranlearner.composeapp.generated.resources.surah_name_32
import quranlearner.composeapp.generated.resources.surah_name_33
import quranlearner.composeapp.generated.resources.surah_name_34
import quranlearner.composeapp.generated.resources.surah_name_35
import quranlearner.composeapp.generated.resources.surah_name_36
import quranlearner.composeapp.generated.resources.surah_name_37
import quranlearner.composeapp.generated.resources.surah_name_38
import quranlearner.composeapp.generated.resources.surah_name_39
import quranlearner.composeapp.generated.resources.surah_name_4
import quranlearner.composeapp.generated.resources.surah_name_40
import quranlearner.composeapp.generated.resources.surah_name_41
import quranlearner.composeapp.generated.resources.surah_name_42
import quranlearner.composeapp.generated.resources.surah_name_43
import quranlearner.composeapp.generated.resources.surah_name_44
import quranlearner.composeapp.generated.resources.surah_name_45
import quranlearner.composeapp.generated.resources.surah_name_46
import quranlearner.composeapp.generated.resources.surah_name_47
import quranlearner.composeapp.generated.resources.surah_name_48
import quranlearner.composeapp.generated.resources.surah_name_49
import quranlearner.composeapp.generated.resources.surah_name_5
import quranlearner.composeapp.generated.resources.surah_name_50
import quranlearner.composeapp.generated.resources.surah_name_51
import quranlearner.composeapp.generated.resources.surah_name_52
import quranlearner.composeapp.generated.resources.surah_name_53
import quranlearner.composeapp.generated.resources.surah_name_54
import quranlearner.composeapp.generated.resources.surah_name_55
import quranlearner.composeapp.generated.resources.surah_name_56
import quranlearner.composeapp.generated.resources.surah_name_57
import quranlearner.composeapp.generated.resources.surah_name_58
import quranlearner.composeapp.generated.resources.surah_name_59
import quranlearner.composeapp.generated.resources.surah_name_6
import quranlearner.composeapp.generated.resources.surah_name_60
import quranlearner.composeapp.generated.resources.surah_name_61
import quranlearner.composeapp.generated.resources.surah_name_62
import quranlearner.composeapp.generated.resources.surah_name_63
import quranlearner.composeapp.generated.resources.surah_name_64
import quranlearner.composeapp.generated.resources.surah_name_65
import quranlearner.composeapp.generated.resources.surah_name_66
import quranlearner.composeapp.generated.resources.surah_name_67
import quranlearner.composeapp.generated.resources.surah_name_68
import quranlearner.composeapp.generated.resources.surah_name_69
import quranlearner.composeapp.generated.resources.surah_name_7
import quranlearner.composeapp.generated.resources.surah_name_70
import quranlearner.composeapp.generated.resources.surah_name_71
import quranlearner.composeapp.generated.resources.surah_name_72
import quranlearner.composeapp.generated.resources.surah_name_73
import quranlearner.composeapp.generated.resources.surah_name_74
import quranlearner.composeapp.generated.resources.surah_name_75
import quranlearner.composeapp.generated.resources.surah_name_76
import quranlearner.composeapp.generated.resources.surah_name_77
import quranlearner.composeapp.generated.resources.surah_name_78
import quranlearner.composeapp.generated.resources.surah_name_79
import quranlearner.composeapp.generated.resources.surah_name_8
import quranlearner.composeapp.generated.resources.surah_name_80
import quranlearner.composeapp.generated.resources.surah_name_81
import quranlearner.composeapp.generated.resources.surah_name_82
import quranlearner.composeapp.generated.resources.surah_name_83
import quranlearner.composeapp.generated.resources.surah_name_84
import quranlearner.composeapp.generated.resources.surah_name_85
import quranlearner.composeapp.generated.resources.surah_name_86
import quranlearner.composeapp.generated.resources.surah_name_87
import quranlearner.composeapp.generated.resources.surah_name_88
import quranlearner.composeapp.generated.resources.surah_name_89
import quranlearner.composeapp.generated.resources.surah_name_9
import quranlearner.composeapp.generated.resources.surah_name_90
import quranlearner.composeapp.generated.resources.surah_name_91
import quranlearner.composeapp.generated.resources.surah_name_92
import quranlearner.composeapp.generated.resources.surah_name_93
import quranlearner.composeapp.generated.resources.surah_name_94
import quranlearner.composeapp.generated.resources.surah_name_95
import quranlearner.composeapp.generated.resources.surah_name_96
import quranlearner.composeapp.generated.resources.surah_name_97
import quranlearner.composeapp.generated.resources.surah_name_98
import quranlearner.composeapp.generated.resources.surah_name_99

/** Bundled calligraphy art for each surah name (falls back to null if out of range). */
@Composable
fun rememberSurahNamePainter(surahNumber: Int): Painter? = when (surahNumber) {
        1 -> painterResource(Res.drawable.surah_name_1)
        2 -> painterResource(Res.drawable.surah_name_2)
        3 -> painterResource(Res.drawable.surah_name_3)
        4 -> painterResource(Res.drawable.surah_name_4)
        5 -> painterResource(Res.drawable.surah_name_5)
        6 -> painterResource(Res.drawable.surah_name_6)
        7 -> painterResource(Res.drawable.surah_name_7)
        8 -> painterResource(Res.drawable.surah_name_8)
        9 -> painterResource(Res.drawable.surah_name_9)
        10 -> painterResource(Res.drawable.surah_name_10)
        11 -> painterResource(Res.drawable.surah_name_11)
        12 -> painterResource(Res.drawable.surah_name_12)
        13 -> painterResource(Res.drawable.surah_name_13)
        14 -> painterResource(Res.drawable.surah_name_14)
        15 -> painterResource(Res.drawable.surah_name_15)
        16 -> painterResource(Res.drawable.surah_name_16)
        17 -> painterResource(Res.drawable.surah_name_17)
        18 -> painterResource(Res.drawable.surah_name_18)
        19 -> painterResource(Res.drawable.surah_name_19)
        20 -> painterResource(Res.drawable.surah_name_20)
        21 -> painterResource(Res.drawable.surah_name_21)
        22 -> painterResource(Res.drawable.surah_name_22)
        23 -> painterResource(Res.drawable.surah_name_23)
        24 -> painterResource(Res.drawable.surah_name_24)
        25 -> painterResource(Res.drawable.surah_name_25)
        26 -> painterResource(Res.drawable.surah_name_26)
        27 -> painterResource(Res.drawable.surah_name_27)
        28 -> painterResource(Res.drawable.surah_name_28)
        29 -> painterResource(Res.drawable.surah_name_29)
        30 -> painterResource(Res.drawable.surah_name_30)
        31 -> painterResource(Res.drawable.surah_name_31)
        32 -> painterResource(Res.drawable.surah_name_32)
        33 -> painterResource(Res.drawable.surah_name_33)
        34 -> painterResource(Res.drawable.surah_name_34)
        35 -> painterResource(Res.drawable.surah_name_35)
        36 -> painterResource(Res.drawable.surah_name_36)
        37 -> painterResource(Res.drawable.surah_name_37)
        38 -> painterResource(Res.drawable.surah_name_38)
        39 -> painterResource(Res.drawable.surah_name_39)
        40 -> painterResource(Res.drawable.surah_name_40)
        41 -> painterResource(Res.drawable.surah_name_41)
        42 -> painterResource(Res.drawable.surah_name_42)
        43 -> painterResource(Res.drawable.surah_name_43)
        44 -> painterResource(Res.drawable.surah_name_44)
        45 -> painterResource(Res.drawable.surah_name_45)
        46 -> painterResource(Res.drawable.surah_name_46)
        47 -> painterResource(Res.drawable.surah_name_47)
        48 -> painterResource(Res.drawable.surah_name_48)
        49 -> painterResource(Res.drawable.surah_name_49)
        50 -> painterResource(Res.drawable.surah_name_50)
        51 -> painterResource(Res.drawable.surah_name_51)
        52 -> painterResource(Res.drawable.surah_name_52)
        53 -> painterResource(Res.drawable.surah_name_53)
        54 -> painterResource(Res.drawable.surah_name_54)
        55 -> painterResource(Res.drawable.surah_name_55)
        56 -> painterResource(Res.drawable.surah_name_56)
        57 -> painterResource(Res.drawable.surah_name_57)
        58 -> painterResource(Res.drawable.surah_name_58)
        59 -> painterResource(Res.drawable.surah_name_59)
        60 -> painterResource(Res.drawable.surah_name_60)
        61 -> painterResource(Res.drawable.surah_name_61)
        62 -> painterResource(Res.drawable.surah_name_62)
        63 -> painterResource(Res.drawable.surah_name_63)
        64 -> painterResource(Res.drawable.surah_name_64)
        65 -> painterResource(Res.drawable.surah_name_65)
        66 -> painterResource(Res.drawable.surah_name_66)
        67 -> painterResource(Res.drawable.surah_name_67)
        68 -> painterResource(Res.drawable.surah_name_68)
        69 -> painterResource(Res.drawable.surah_name_69)
        70 -> painterResource(Res.drawable.surah_name_70)
        71 -> painterResource(Res.drawable.surah_name_71)
        72 -> painterResource(Res.drawable.surah_name_72)
        73 -> painterResource(Res.drawable.surah_name_73)
        74 -> painterResource(Res.drawable.surah_name_74)
        75 -> painterResource(Res.drawable.surah_name_75)
        76 -> painterResource(Res.drawable.surah_name_76)
        77 -> painterResource(Res.drawable.surah_name_77)
        78 -> painterResource(Res.drawable.surah_name_78)
        79 -> painterResource(Res.drawable.surah_name_79)
        80 -> painterResource(Res.drawable.surah_name_80)
        81 -> painterResource(Res.drawable.surah_name_81)
        82 -> painterResource(Res.drawable.surah_name_82)
        83 -> painterResource(Res.drawable.surah_name_83)
        84 -> painterResource(Res.drawable.surah_name_84)
        85 -> painterResource(Res.drawable.surah_name_85)
        86 -> painterResource(Res.drawable.surah_name_86)
        87 -> painterResource(Res.drawable.surah_name_87)
        88 -> painterResource(Res.drawable.surah_name_88)
        89 -> painterResource(Res.drawable.surah_name_89)
        90 -> painterResource(Res.drawable.surah_name_90)
        91 -> painterResource(Res.drawable.surah_name_91)
        92 -> painterResource(Res.drawable.surah_name_92)
        93 -> painterResource(Res.drawable.surah_name_93)
        94 -> painterResource(Res.drawable.surah_name_94)
        95 -> painterResource(Res.drawable.surah_name_95)
        96 -> painterResource(Res.drawable.surah_name_96)
        97 -> painterResource(Res.drawable.surah_name_97)
        98 -> painterResource(Res.drawable.surah_name_98)
        99 -> painterResource(Res.drawable.surah_name_99)
        100 -> painterResource(Res.drawable.surah_name_100)
        101 -> painterResource(Res.drawable.surah_name_101)
        102 -> painterResource(Res.drawable.surah_name_102)
        103 -> painterResource(Res.drawable.surah_name_103)
        104 -> painterResource(Res.drawable.surah_name_104)
        105 -> painterResource(Res.drawable.surah_name_105)
        106 -> painterResource(Res.drawable.surah_name_106)
        107 -> painterResource(Res.drawable.surah_name_107)
        108 -> painterResource(Res.drawable.surah_name_108)
        109 -> painterResource(Res.drawable.surah_name_109)
        110 -> painterResource(Res.drawable.surah_name_110)
        111 -> painterResource(Res.drawable.surah_name_111)
        112 -> painterResource(Res.drawable.surah_name_112)
        113 -> painterResource(Res.drawable.surah_name_113)
        114 -> painterResource(Res.drawable.surah_name_114)
    else -> null
}
