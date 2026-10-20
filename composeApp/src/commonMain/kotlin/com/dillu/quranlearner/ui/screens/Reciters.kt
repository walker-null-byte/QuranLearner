package com.dillu.quranlearner.ui.screens

/**
 * A reciter available in the app.
 *
 * [folder] is the EveryAyah.com folder used for ayah-by-ayah audio.
 * [dataSaverFolder] is the lower-bitrate EveryAyah folder used when "Data saver" is on
 * (equal to [folder] where no lower-bitrate variant exists).
 * [fullSurahServer] is the mp3quran.net server used for full-surah playback and downloads.
 *
 * Folder names below were verified against everyayah.com (all return HTTP 200).
 */
data class ReciterOption(
    val id: String,
    val name: String,
    val folder: String,
    val dataSaverFolder: String = folder,
    val fullSurahServer: String = "",
)

val AVAILABLE_RECITERS = listOf(
    ReciterOption("alafasy", "Mishary Rashid Al-Afasy", "Alafasy_128kbps", "Alafasy_64kbps", "https://server8.mp3quran.net/afs/"),
    ReciterOption("husary", "Mahmoud Khalil Al-Husary", "Husary_128kbps", "Husary_64kbps", "https://server13.mp3quran.net/husr/"),
    ReciterOption("minshawi_murattal", "Mohamed Siddiq Al-Minshawi", "Minshawy_Murattal_128kbps", "Minshawy_Murattal_128kbps", "https://server10.mp3quran.net/minsh/"),
    ReciterOption("abdulbasit_murattal", "Abdul Basit (Murattal)", "Abdul_Basit_Murattal_192kbps", "Abdul_Basit_Murattal_64kbps", "https://server7.mp3quran.net/basit/"),
    ReciterOption("sudais", "Abdur-Rahman As-Sudais", "Abdurrahmaan_As-Sudais_192kbps", "Abdurrahmaan_As-Sudais_64kbps", "https://server11.mp3quran.net/sds/"),
    ReciterOption("shuraim", "Saud Ash-Shuraim", "Saood_ash-Shuraym_128kbps", "Saood_ash-Shuraym_64kbps", "https://server7.mp3quran.net/shrm/"),
    ReciterOption("ajamy", "Ahmed Al-Ajamy", "Ahmed_ibn_Ali_al-Ajamy_128kbps_ketaballah.net", "Ahmed_ibn_Ali_al-Ajamy_64kbps_QuranExplorer.Com", "https://server10.mp3quran.net/ajm/"),
    ReciterOption("maher", "Maher Al-Muaiqly", "MaherAlMuaiqly128kbps", "Maher_AlMuaiqly_64kbps", "https://server12.mp3quran.net/maher/"),
)

/** EveryAyah folder for [reciterId], applying the data-saver variant when [dataSaver] is on. */
fun reciterFolderFor(reciterId: String, dataSaver: Boolean): String {
    val reciter = AVAILABLE_RECITERS.find { it.id == reciterId } ?: AVAILABLE_RECITERS.first()
    return if (dataSaver) reciter.dataSaverFolder else reciter.folder
}

/** Base URL of the full-surah server for an EveryAyah folder (data-saver folders map back to their reciter). */
fun fullSurahServerFor(reciterFolder: String): String =
    AVAILABLE_RECITERS.find { it.folder == reciterFolder || it.dataSaverFolder == reciterFolder }
        ?.fullSurahServer
        ?: AVAILABLE_RECITERS.first().fullSurahServer

/** Settings key under which downloaded surah numbers are stored for a specific reciter folder. */
fun downloadedSurahsKey(reciterFolder: String): String = "downloaded_surahs_$reciterFolder"
