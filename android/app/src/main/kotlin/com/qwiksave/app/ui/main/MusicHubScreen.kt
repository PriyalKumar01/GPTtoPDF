package com.qwiksave.app.ui.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.qwiksave.app.data.UserPreferences
import com.qwiksave.app.sdk.QwikSaveClient
import com.qwiksave.app.service.MusicController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Data Model for Music Tracks (90s Classics, Trending, Punjabi, Bhojpuri & Travel Songs)
 */
data class SongTrack(
    val id: String,
    val title: String,
    val artist: String,
    val albumOrMovie: String,
    val durationText: String,
    val durationSec: Int,
    val streamUrl: String,
    val coverUrl: String,
    val category: String // "90s", "Trending", "Travel", "Punjabi", "Bhojpuri", "Custom"
)

/**
 * Extensive Curated 70s, 80s, 90s, 2000s & Evergreen Hits Catalog
 * Verified high-quality direct audio streams with zero buffering.
 */
val CURATED_SONGS_CATALOG = listOf(
    SongTrack(
        id = "K5VHIn3A",
        title = "Yeh Duniya Yeh Mehfil",
        artist = "Mohammed Rafi",
        albumOrMovie = "Heer Raanjha",
        durationText = "7:09",
        durationSec = 429,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010910080036110.mp3",
        coverUrl = "https://c.saavncdn.com/780/Heer-Raanjha-Hindi-1970-20190710084802-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "ksP-TLcH",
        title = "Pyaar Ke Kagaz Pe",
        artist = "Abhijeet Bhattacharya, Sadhana Sargam",
        albumOrMovie = "Jigar",
        durationText = "7:04",
        durationSec = 424,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010911261743316.mp3",
        coverUrl = "https://c.saavncdn.com/046/Jigar-Hindi-1992-20261103214604-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "UxIHVsa1",
        title = "Kasam Se",
        artist = "Udit Narayan, Alka Yagnik",
        albumOrMovie = "Jaanwar",
        durationText = "5:09",
        durationSec = 309,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010912020948510.mp3",
        coverUrl = "https://c.saavncdn.com/630/Jaanwar-Hindi-1999-20210407130950-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "VTrnhjE3",
        title = "Tham Ke Baras Female",
        artist = "Alka Yagnik",
        albumOrMovie = "Mere Mehboob",
        durationText = "6:51",
        durationSec = 411,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010911261764373.mp3",
        coverUrl = "https://c.saavncdn.com/348/Mere-Mehboob-Hindi-2008-20261103220851-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "AYHP28Sh",
        title = "Phir Aur Kya Chahiye (From Zara Hatke Zara Bachke)",
        artist = "Sachin-Jigar, Arijit Singh",
        albumOrMovie = "Zara Hatke Zara Bachke",
        durationText = "4:26",
        durationSec = 266,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010910081922589.mp3",
        coverUrl = "https://c.saavncdn.com/336/Zara-Hatke-Zara-Bachke-Hindi-2023-20250129153124-500x500.jpg",
        category = "Trending"
    ),
    SongTrack(
        id = "ZUKuoGM3",
        title = "Bahut Jatate Ho Pyar",
        artist = "Alka Yagnik, Mohammed Aziz",
        albumOrMovie = "Aadmi Khilona Hai (Original Motion Picture Soundtrack)",
        durationText = "7:11",
        durationSec = 431,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010912020947981.mp3",
        coverUrl = "https://c.saavncdn.com/361/Aadmi-Khilona-Hai-Original-Motion-Picture-Soundtrack-Hindi-1993-20250819191246-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "36bU1G5Y",
        title = "Mat Kar Itna Guroor",
        artist = "Alka Yagnik, Pankaj Udhas",
        albumOrMovie = "Aadmi Khilona Hai (Original Motion Picture Soundtrack)",
        durationText = "6:47",
        durationSec = 407,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010912020947984.mp3",
        coverUrl = "https://c.saavncdn.com/361/Aadmi-Khilona-Hai-Original-Motion-Picture-Soundtrack-Hindi-1993-20250819191246-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "s7QNVZKi",
        title = "Aadmi Khilona Hai",
        artist = "Alka Yagnik",
        albumOrMovie = "Aadmi Khilona Hai (Original Motion Picture Soundtrack)",
        durationText = "5:03",
        durationSec = 303,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010912020947985.mp3",
        coverUrl = "https://c.saavncdn.com/361/Aadmi-Khilona-Hai-Original-Motion-Picture-Soundtrack-Hindi-1993-20250819191246-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "OICrCdcO",
        title = "I Am Sorry",
        artist = "Mukul Agarwal, Alka Yagnik",
        albumOrMovie = "Sangraam (Original Motion Picture Soundtrack)",
        durationText = "6:09",
        durationSec = 369,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010912020949024.mp3",
        coverUrl = "https://c.saavncdn.com/533/Sangraam-Original-Motion-Picture-Soundtrack-Hindi-1993-20250912131400-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "6poQxFc7",
        title = "Aap Ko Dekh Kar",
        artist = "Kumar Sanu, Alka Yagnik",
        albumOrMovie = "Divya Shakti",
        durationText = "6:33",
        durationSec = 393,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010910140561620.mp3",
        coverUrl = "https://c.saavncdn.com/019/Divya-Shakti-Hindi-2017-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "irLm7PdJ",
        title = "Tere Aagey Peechey",
        artist = "Kumar Sanu, Alka Yagnik",
        albumOrMovie = "Hum To Mohabbat Karega",
        durationText = "7:02",
        durationSec = 422,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010910080025693.mp3",
        coverUrl = "https://c.saavncdn.com/934/Hum-To-Mohabbat-Karega-Hindi-2000-20171009061149-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "xk5n6-Ry",
        title = "Wafa Na Raas Aayee Tujhe O Harjaee",
        artist = "Nitin Mukesh",
        albumOrMovie = "Bewafa Sanam",
        durationText = "6:10",
        durationSec = 370,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010910090396034.mp3",
        coverUrl = "https://c.saavncdn.com/019/Bewafa-Sanam-Hindi-1995-20230124185643-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "pe_LkMCX",
        title = "Woh Kisi Aur Kisi Aur Se Milke",
        artist = "Agam Kumar Nigam",
        albumOrMovie = "Phir Bewafaai Deceived In Love",
        durationText = "6:27",
        durationSec = 387,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010910091606550.mp3",
        coverUrl = "https://c.saavncdn.com/887/Phir-Bewafaai-Deceived-In-Love-2007-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "DPpCSV_r",
        title = "Dono Ke Husn Mein",
        artist = "Kumar Sanu, Alka Yagnik",
        albumOrMovie = "Dilwale Kabhi Na Hare",
        durationText = "5:20",
        durationSec = 320,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010910140647878.mp3",
        coverUrl = "https://c.saavncdn.com/502/Dilwale-Kabhi-Na-Hare-Hindi-1992-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "HyqbmeHP",
        title = "Tumne Rakh To Lee Tasveer Hamari",
        artist = "Anuradha Paudwal, Pankaj Udhas",
        albumOrMovie = "Lal Dupatta Malmal Ka",
        durationText = "5:15",
        durationSec = 315,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010910090117584.mp3",
        coverUrl = "https://c.saavncdn.com/921/Lal-Dupatta-Malmal-Ka-Hindi-1990-20221208021455-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "UeXh39z0",
        title = "Mohabbat Inayat Karam Dekhte Hain",
        artist = "Anuradha Paudwal, Pankaj Udhas",
        albumOrMovie = "Bahaar Aane Tak",
        durationText = "6:58",
        durationSec = 418,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010910090343063.mp3",
        coverUrl = "https://c.saavncdn.com/752/Bahaar-Aane-Tak-Hindi-1990-20221207235431-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "KDaVdEp7",
        title = "Woh To Hai Albela",
        artist = "Kumar Sanu, Devaki Pandit",
        albumOrMovie = "Kabhi Haan Kabhi Naa",
        durationText = "5:09",
        durationSec = 309,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010910140014978.mp3",
        coverUrl = "https://c.saavncdn.com/051/Kabhi-Haan-Kabhi-Naa-Hindi-1999-20200620121926-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "JY15hO0_",
        title = "Koi Na Koi Chahiye",
        artist = "Vinod Rathod",
        albumOrMovie = "Deewana",
        durationText = "6:23",
        durationSec = 383,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010912020948947.mp3",
        coverUrl = "https://c.saavncdn.com/793/Deewana-Hindi-1992-20221215163751-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "n3CdyaXl",
        title = "Na Milo Kahin Pyar",
        artist = "Sonu Nigam, Kavita Krishnamurthy",
        albumOrMovie = "Badal",
        durationText = "6:43",
        durationSec = 403,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010912023396848.mp3",
        coverUrl = "https://c.saavncdn.com/533/Badal-Hindi-1999-20221119140222-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "0fHe7Cqu",
        title = "Dil Kehta Hai (From Akele Hum Akele Tum)",
        artist = "Alka Yagnik, Kumar Sanu",
        albumOrMovie = "Majrooh Sultanpuri's Bollywood Collection",
        durationText = "6:55",
        durationSec = 415,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010912020948014.mp3",
        coverUrl = "https://c.saavncdn.com/614/Majrooh-Sultanpuri-s-Bollywood-Collection-Hindi-2016-20260331205731-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "Cmki9uze",
        title = "Main Tera Ashiq Hoon",
        artist = "Roop Kumar Rathod",
        albumOrMovie = "Gumraah",
        durationText = "6:57",
        durationSec = 417,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010910080036065.mp3",
        coverUrl = "https://c.saavncdn.com/657/Gumrah-Hindi-1993-20171005103813-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "LsnT84OS",
        title = "Dil Jab Se Toot Gaya",
        artist = "Pankaj Udhas",
        albumOrMovie = "Salaami (Original Motion Picture Soundtrack)",
        durationText = "5:33",
        durationSec = 333,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010912020948717.mp3",
        coverUrl = "https://c.saavncdn.com/151/Salaami-Original-Motion-Picture-Soundtrack-Hindi-1993-20250815044606-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "CGfOlner",
        title = "Ae Kash Ke Hum",
        artist = "Kumar Sanu",
        albumOrMovie = "Kabhi Haan Kabhi Naa",
        durationText = "5:07",
        durationSec = 307,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010910140014973.mp3",
        coverUrl = "https://c.saavncdn.com/051/Kabhi-Haan-Kabhi-Naa-Hindi-1999-20200620121926-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "EkvSn2po",
        title = "To Chalun",
        artist = "Roop Kumar Rathod",
        albumOrMovie = "Border (Original Motion Picture Soundtrack)",
        durationText = "7:38",
        durationSec = 458,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010912020948985.mp3",
        coverUrl = "https://c.saavncdn.com/843/Border-Original-Motion-Picture-Soundtrack-Hindi-1999-20250912124905-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "JJrg3uv5",
        title = "Humko Mohabbat Dhoond Rahi Thi (Kitne Door Kitne Paas / Soundtrack Version)",
        artist = "Roop Kumar Rathod, Jayshri Shivram",
        albumOrMovie = "Kitne Door Kitne Paas",
        durationText = "6:14",
        durationSec = 374,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010912291576113.mp3",
        coverUrl = "https://c.saavncdn.com/328/Kitne-Door-Kitne-Paas-Hindi-2008-20201126220008-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "_ilNTn4a",
        title = "Chhupana Bhi Nahin Aata",
        artist = "Pankaj Udhas",
        albumOrMovie = "Golden Era",
        durationText = "6:59",
        durationSec = 419,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010912020947956.mp3",
        coverUrl = "https://c.saavncdn.com/185/Golden-Era-Hindi-2026-20260813171634-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "U9ac0v-y",
        title = "Yeh Teri Aankhen Jhuki Jhuki",
        artist = "Jatin-Lalit",
        albumOrMovie = "Fareb",
        durationText = "3:22",
        durationSec = 202,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010910440719002.mp3",
        coverUrl = "https://c.saavncdn.com/887/Fareb-Hindi-2018-20180220-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "rkXa7yEx",
        title = "Kaash Kahin Aisa Hota",
        artist = "Kumar Sanu",
        albumOrMovie = "Heart Touching Love Songs",
        durationText = "5:05",
        durationSec = 305,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010912020948650.mp3",
        coverUrl = "https://c.saavncdn.com/700/Heart-Touching-Love-Songs-Hindi-2021-20260401031231-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "TOyzlrBQ",
        title = "Kuch Baatein",
        artist = "Payal Dev, Jubin Nautiyal",
        albumOrMovie = "Kuch Baatein",
        durationText = "4:45",
        durationSec = 285,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010910091493655.mp3",
        coverUrl = "https://c.saavncdn.com/241/Kuch-Baatein-Hindi-2022-20220310031017-500x500.jpg",
        category = "Trending"
    ),
    SongTrack(
        id = "CKQKCTpJ",
        title = "Kahan Laya Mera Yaar",
        artist = "Suresh Wadkar, Alka Yagnik",
        albumOrMovie = "Suresh Wadkars Bollywood Hits",
        durationText = "7:44",
        durationSec = 464,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010912261611121.mp3",
        coverUrl = "https://c.saavncdn.com/578/Suresh-Wadkars-Bollywood-Hits-Hindi-2021-20260401031228-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "VDE7p9P3",
        title = "Jaan - E - Jigar Jaaneman",
        artist = "Anuradha Paudwal, Kumar Sanu",
        albumOrMovie = "Aashiqui",
        durationText = "5:10",
        durationSec = 310,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010910090109224.mp3",
        coverUrl = "https://c.saavncdn.com/088/Aashiqui-Hindi-1989-20241225141825-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "1Uu08jB-",
        title = "Agar Aasman Tak Mere Haat Jaate",
        artist = "Bollywood Artist",
        albumOrMovie = "Meherbaan",
        durationText = "4:57",
        durationSec = 297,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010910090301251.mp3",
        coverUrl = "https://c.saavncdn.com/003/Meherbaan-Hindi-1993-20221208022345-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "CvUYEydi",
        title = "Maa Ka Bulaava Aayega",
        artist = "Bollywood Artist",
        albumOrMovie = "Sandeep Aur Pinky Faraar",
        durationText = "0:56",
        durationSec = 56,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010910391199371.mp3",
        coverUrl = "https://c.saavncdn.com/149/Sandeep-Aur-Pinky-Faraar-Hindi-2021-20210309125304-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "BAiADMge",
        title = "Pehli Baarish Mein",
        artist = "Bollywood Artist",
        albumOrMovie = "Pehli Baarish Mein",
        durationText = "4:00",
        durationSec = 240,
        streamUrl = "https://jiotunepreview.jio.com/content/Converted/010910091967386.mp3",
        coverUrl = "https://c.saavncdn.com/726/Pehli-Baarish-Mein-Hindi-2023-20230615181015-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "09Tm7xXj",
        title = "Jise Dekh Mera Dil Dhadke",
        artist = "Kumar Sanu",
        albumOrMovie = "Phool Aur Kaante",
        durationText = "6:36",
        durationSec = 396,
        streamUrl = "https://preview.saavncdn.com/882/DmmMTnaIlfkczAPzx86UZGh9oHYLfYfq_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/882/Phool-Aur-Kaante-Hindi-1991-20240408213850-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "0fUBGGzb",
        title = "Aayega Bulava to Jana Padega",
        artist = "Moinudin Manchala",
        albumOrMovie = "Aayega Bulava to Jana Padega",
        durationText = "6:09",
        durationSec = 369,
        streamUrl = "https://preview.saavncdn.com/691/fLAyv2SmfBbFfBRI7uCMcTUsgUzOFarualhliG_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/691/Aayega-Bulava-to-Jana-Padega-Hindi-2020-20200925060524-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "xazV6TRo",
        title = "Agar Aasman Tak Mera",
        artist = "Anuradha Paudwal, Sonu Nigam",
        albumOrMovie = "Naaraaz",
        durationText = "6:16",
        durationSec = 376,
        streamUrl = "https://preview.saavncdn.com/362/NpGhmGn0bWcjaDXc3JT9Uydrzt4Am6gPTF3xrJN_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/362/Naaraaz-Hindi-1994-20240408213718-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "4mhcWo1B",
        title = "Yeh Aankhen Hai Aaina",
        artist = "Kumar Sanu, Alka Yagnik",
        albumOrMovie = "Stunttman",
        durationText = "5:38",
        durationSec = 338,
        streamUrl = "https://preview.saavncdn.com/407/aqyD3eFuCoHc5VCCNPZIrfPJj9rYMl9AAc_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/407/Stunttman-Hindi-1994-20240408213744-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "KhF4WaRa",
        title = "Kitni Hasrat Hai Humein",
        artist = "Kumar Sanu, Sadhana Sargam",
        albumOrMovie = "Sainik",
        durationText = "6:32",
        durationSec = 392,
        streamUrl = "https://preview.saavncdn.com/179/2U6Lc0eVyeoLqrH07nRwnW62WnLdOLLdQhZ_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/179/Sainik-Hindi-1993-20240408213840-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "BkLmSY4P",
        title = "Milne Ki Tum Koshish Karna",
        artist = "Asha Bhosle, Kumar Sanu",
        albumOrMovie = "Dil Ka Kya Kasoor",
        durationText = "6:03",
        durationSec = 363,
        streamUrl = "https://preview.saavncdn.com/572/veG5oLn18v2h3elPv4PIlBtDOW59J2wnOU9Ju7ypXAAd1K_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/572/Dil-Ka-Kya-Kasoor-Hindi-1992-20261028195249-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "E75lRl_D",
        title = "Ghoonghat Ki Aadh Se",
        artist = "Kumar Sanu, Alka Yagnik",
        albumOrMovie = "Hum Hain Rahi Pyar Ke",
        durationText = "6:16",
        durationSec = 376,
        streamUrl = "https://preview.saavncdn.com/080/3eVMZO679OCICVNgXMbpQ1QrcqYIbhZd_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/080/Hum-Hain-Rahi-Pyar-Ke-Hindi-1993-20240408213752-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "SNkMikLw",
        title = "Mere Dil Ki Galiyon Mein",
        artist = "Alka Yagnik, Suresh Wadkar",
        albumOrMovie = "Banjaran",
        durationText = "5:43",
        durationSec = 343,
        streamUrl = "https://preview.saavncdn.com/183/WP69PksplgK7cPmhDy4UaOgOmS57W7EZIS_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/183/Banjaran-Hindi-1991-20240408213830-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "JM-Wx2OR",
        title = "Do Baatein Ho Sakti Hai",
        artist = "Kumar Sanu",
        albumOrMovie = "Imtihan",
        durationText = "7:57",
        durationSec = 477,
        streamUrl = "https://preview.saavncdn.com/458/CeBmojK4nTi5zqIIzGvK4JtzabI2vzgJghSGOO_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/458/Imtihan-Hindi-1994-20240408214016-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "TsxM1Czn",
        title = "Baadalon Mein Chup Raha Chand",
        artist = "Alka Yagnik, Kumar Sanu",
        albumOrMovie = "Phir Teri Kahani Yaad Aayee",
        durationText = "6:14",
        durationSec = 374,
        streamUrl = "https://preview.saavncdn.com/643/2lqxRdcoEXq5T2ggoid58aYfOixyBGPZ8GttjKUKj_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/643/Phir-Teri-Kahani-Yaad-Aayee-Hindi-1993-20240408213831-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "2o5nK19J",
        title = "Pehli Baarish Main Aur Tu",
        artist = "Anuradha Paudwal, Kumar Sanu",
        albumOrMovie = "Phool Aur Kaante",
        durationText = "4:52",
        durationSec = 292,
        streamUrl = "https://preview.saavncdn.com/882/LiA4We1rV022tmTjRdH5JqifAA6mxybhL9_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/882/Phool-Aur-Kaante-Hindi-1991-20240408213850-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "NZtJ-TeX",
        title = "Saathiya Bin Tere Dil Mane Na",
        artist = "Kumar Sanu, Alka Yagnik",
        albumOrMovie = "Himmat",
        durationText = "6:41",
        durationSec = 401,
        streamUrl = "https://preview.saavncdn.com/992/Kmu2ON8h1hfUH6rAkWhelK3mAQZhOnuQL4nhtnn8JN_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/992/Himmat-Hindi-1996-20240408222459-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "kXdxyHhl",
        title = "Honthon Pe Bas",
        artist = "Lata Mangeshkar, Kumar Sanu",
        albumOrMovie = "Yeh Dillagi",
        durationText = "4:52",
        durationSec = 292,
        streamUrl = "https://preview.saavncdn.com/669/3PnV7WSeRgADNpQDzjl5P3UPTW0Y1NSg41Z_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/669/Yeh-Dillagi-Hindi-1994-20240408213825-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "XAsHBe8Z",
        title = "Dheere Dheere Pyar Ko",
        artist = "Alka Yagnik, Kumar Sanu",
        albumOrMovie = "Phool Aur Kaante",
        durationText = "5:28",
        durationSec = 328,
        streamUrl = "https://preview.saavncdn.com/882/Jm2rg5DrV8GmWLy8zENKgFwA4JGJPtLmscpaYqs_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/882/Phool-Aur-Kaante-Hindi-1991-20240408213850-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "2aBt65_8",
        title = "Dil Mein Sanam Ki Soorat",
        artist = "Alka Yagnik, Kumar Sanu",
        albumOrMovie = "Phir Teri Kahani Yaad Aayee",
        durationText = "6:06",
        durationSec = 366,
        streamUrl = "https://preview.saavncdn.com/643/zaWzaKCnDGzbXQldjhMLleFLXMQTdfObrWcDiPUW_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/643/Phir-Teri-Kahani-Yaad-Aayee-Hindi-1993-20240408213831-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "hS9voN-F",
        title = "Chatri Na Khol Barsaat Mein",
        artist = "Kumar Sanu, Poornima",
        albumOrMovie = "Gopi Kishan",
        durationText = "5:52",
        durationSec = 352,
        streamUrl = "https://preview.saavncdn.com/886/gquWzNIj4GNet0ciJCUkKyWUPnuuYxbAnDsoftg6_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/886/Gopi-Kishan-Hindi-1994-20240408213920-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "1N_JmZnW",
        title = "Rone Na Dijiyega",
        artist = "Kumar Sanu",
        albumOrMovie = "Jaan Tere Naam",
        durationText = "4:51",
        durationSec = 291,
        streamUrl = "https://preview.saavncdn.com/172/k7Ct4SzWbSSuZqY3akuaKVt5LJ6PmFKkD_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/172/Jaan-Tere-Naam-Hindi-1992-20240408213836-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "ZDF9M4pc",
        title = "Aankhon Mein Basaya Tha",
        artist = "Kumar Sanu",
        albumOrMovie = "Takkar",
        durationText = "5:35",
        durationSec = 335,
        streamUrl = "https://preview.saavncdn.com/476/KivnooGQWGbZtBsFZX5dNzijjJerTf51AzIOOR_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/476/Takkar-Hindi-1995-20240408221331-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "1Njr-QwB",
        title = "Kitna Majboor Ho Gaya",
        artist = "Kumar Sanu",
        albumOrMovie = "Dil Ka Rishta",
        durationText = "1:35",
        durationSec = 95,
        streamUrl = "https://preview.saavncdn.com/589/Qa0xZsfUqCBpd8GOyGIcONQAyAxNtrwCMo0XFnjSlOmfIi_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/589/Dil-Ka-Rishta-Hindi-2003-20261105195257-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "RrPXy4Te",
        title = "Aankhon Mein Neendein Na Dil",
        artist = "Alka Yagnik, Kumar Sanu",
        albumOrMovie = "Sanam",
        durationText = "5:59",
        durationSec = 359,
        streamUrl = "https://preview.saavncdn.com/186/avCqAigli170whovFARxm4Ijk9dCnVGdH8XjE6f_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/186/Sanam-Hindi-1997-20240408213817-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "qWtXdkAH",
        title = "Aa Jaana Aa Jaana",
        artist = "Alka Yagnik, Kumar Sanu",
        albumOrMovie = "Coolie No.1",
        durationText = "6:25",
        durationSec = 385,
        streamUrl = "https://preview.saavncdn.com/442/fdRUqZRfexWQXwJz2e1mwm1a3Uw6SmhvSW24haUMeltn_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/442/Coolie-No-1-Hindi-1995-20240408213912-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "ll0aC6vb",
        title = "Kahin Mujhe Pyar Hua Toh Nahin",
        artist = "Alka Yagnik, Kumar Sanu",
        albumOrMovie = "Rang",
        durationText = "7:03",
        durationSec = 423,
        streamUrl = "https://preview.saavncdn.com/056/oSkzxu2k9PAMzQ22YkvDnb8z1vO8GF2xMp1rXgQAbbnilW_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/056/Rang-Hindi-1993-20240408213822-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "l0nLiGmo",
        title = "Dil Jaane Jigar Tujh Pe",
        artist = "Kumar Sanu, Alka Yagnik",
        albumOrMovie = "Saajan Chale Sasural",
        durationText = "5:52",
        durationSec = 352,
        streamUrl = "https://preview.saavncdn.com/124/ZLGTawsTtsiUEt5hG5kHMiIqwsafz08r_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/124/Saajan-Chale-Sasural-Hindi-1996-20240408213758-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "v3-gtiJn",
        title = "Tu Dharti Pe Chaahe Jahan Bhi",
        artist = "Kumar Sanu, Alka Yagnik",
        albumOrMovie = "Jeet",
        durationText = "9:09",
        durationSec = 549,
        streamUrl = "https://preview.saavncdn.com/257/MPs8GccX0ahYMghtHK0o1XgDFl14S1n5xzXOBWvnLVO1E_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/257/Jeet-Hindi-1996-20240408213927-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "VmJ-u6kG",
        title = "Is Tarah Aashiqui Ka (Kumar Sanu Version)",
        artist = "Kumar Sanu",
        albumOrMovie = "Imtihan",
        durationText = "7:23",
        durationSec = 443,
        streamUrl = "https://preview.saavncdn.com/458/aC8kUN6kt0u0OFrVjc6mu4Ltw1rlCZ3xoj4dL4x_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/458/Imtihan-Hindi-1994-20240408214016-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "_xfZKp5a",
        title = "Maine Pyar Tumhi Se Kiya Hai",
        artist = "Anuradha Paudwal, Kumar Sanu",
        albumOrMovie = "Phool Aur Kaante",
        durationText = "7:02",
        durationSec = 422,
        streamUrl = "https://preview.saavncdn.com/882/VAhfMac932TPYujVxP2738gDqPqNLkkAuK_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/882/Phool-Aur-Kaante-Hindi-1991-20240408213850-500x500.jpg",
        category = "90s"
    ),
    SongTrack(
        id = "p07AoO9Q",
        title = "Dil Cheer Ke Dekh",
        artist = "Kumar Sanu",
        albumOrMovie = "Rang",
        durationText = "5:13",
        durationSec = 313,
        streamUrl = "https://preview.saavncdn.com/056/ccJZ1uCb7IJhbn693EswkYsmYNpmsuY82hFNOh8c74jsV_96_p.mp4",
        coverUrl = "https://c.saavncdn.com/056/Rang-Hindi-1993-20240408213822-500x500.jpg",
        category = "90s"
    )
)


/**
 * Famous Hindi Song Lyrics & Colloquial Search Aliases
 * Maps popular lyrics lines, verses, and movie names to their official song title
 */
val FAMOUS_LYRICS_ALIASES = mapOf(
    "apna pata mile na khabar" to "Yeh Duniya Yeh Mehfil",
    "apna pata mile" to "Yeh Duniya Yeh Mehfil",
    "apna pata" to "Yeh Duniya Yeh Mehfil",
    "khabar yaar ki mile" to "Yeh Duniya Yeh Mehfil",
    "dushman ko bhi na aisi saza" to "Yeh Duniya Yeh Mehfil",
    "heer raanjha" to "Yeh Duniya Yeh Mehfil",
    "heer ranjha" to "Yeh Duniya Yeh Mehfil",
    "wafa na raas aayi" to "Wafa Na Raas Aayee",
    "wafa na raas" to "Wafa Na Raas Aayee",
    "woh kisi aur se milke" to "Woh Kisi Aur Kisi Aur Se Milke",
    "woh kisi aur" to "Woh Kisi Aur Kisi Aur Se Milke",
    "milne ki tum koshish" to "Milne Ki Tum Koshish Karna",
    "pyaar ke kagaz pe" to "Pyaar Ke Kagaz Pe",
    "dil ki kalam se" to "Pyaar Ke Kagaz Pe",
    "jise dekh mera dil" to "Jise Dekh Mera Dil Dhadke",
    "tham ke baras" to "Tham Ke Baras",
    "bahut jatate ho pyar" to "Bahut Jatate Ho Pyar",
    "mat kar itna guroor" to "Mat Kar Itna Guroor",
    "gora rang do din" to "Mat Kar Itna Guroor",
    "aadmi khilona hai" to "Aadmi Khilona Hai",
    "i am sorry" to "I Am Sorry",
    "agar aasman tak" to "Agar Aasman Tak",
    "yeh aankhen hai aaina" to "Yeh Aankhen Hai Aaina",
    "aap ko dekh kar" to "Aap Ko Dekh Kar",
    "kitni hasrat hai" to "Kitni Hasrat Hai Humein",
    "tere aagey peechey" to "Tere Aagey Peechey",
    "dono ke husn mein" to "Dono Ke Husn Mein",
    "ghoonghat ki aadh se" to "Ghoonghat Ki Aadh Se",
    "mere dil ki galiyon" to "Mere Dil Ki Galiyon Mein",
    "do baatein ho sakti" to "Do Baatein Ho Sakti Hai",
    "tumne rakh to lee" to "Tumne Rakh To Lee Tasveer Hamari",
    "mohabbat inayat karam" to "Mohabbat Inayat Karam Dekhte Hain",
    "baadalon mein chup" to "Baadalon Mein Chup Raha Chand",
    "pehli baarish" to "Pehli Baarish Main Aur Tu",
    "saathiya bin tere" to "Saathiya Bin Tere Dil Mane Na",
    "honthon pe bas" to "Honthon Pe Bas",
    "dheere dheere pyar ko" to "Dheere Dheere Pyar Ko",
    "woh to hai albela" to "Woh To Hai Albela",
    "dil mein sanam ki soorat" to "Dil Mein Sanam Ki Soorat",
    "chatri na khol" to "Chatri Na Khol Barsaat Mein",
    "koi na koi chahiye" to "Koi Na Koi Chahiye",
    "na milo kahin pyar" to "Na Milo Kahin Pyar",
    "dil kehta hai" to "Dil Kehta Hai",
    "main tera ashiq hoon" to "Main Tera Ashiq Hoon",
    "dil jab se toot gaya" to "Dil Jab Se Toot Gaya",
    "ae kash ke hum" to "Ae Kash Ke Hum",
    "to chalun" to "To Chalun",
    "rone na dijiyega" to "Rone Na Dijiyega",
    "humko mohabbat dhoondh" to "Humko Mohabbat Dhoondh Rahi Thi",
    "chhupana bhi nahin aata" to "Chhupana Bhi Nahin Aata",
    "aankhon mein basaya tha" to "Aankhon Mein Basaya Tha",
    "yeh teri aankhen jhuki" to "Yeh Teri Aankhen Jhuki Jhuki",
    "kitna majboor ho gaya" to "Kitna Majboor Ho Gaya",
    "kaash kahin aisa hota" to "Kaash Kahin Aisa Hota",
    "aankhon mein neendein" to "Aankhon Mein Neendein Na Dil",
    "aa jaana aa jaana" to "Aa Jaana Aa Jaana",
    "kahin mujhe pyar hua" to "Kahin Mujhe Pyar Hua Toh Nahin",
    "kuch baatein" to "Kuch Baatein",
    "kahan laya mera yaar" to "Kahan Laya Mera Yaar",
    "dil jaane jigar" to "Dil Jaane Jigar Tujh Pe",
    "tu dharti pe chaahe" to "Tu Dharti Pe Chaahe Jahan Bhi",
    "is tarah aashiqui ka" to "Is Tarah Aashiqui Ka",
    "jaan e jigar" to "Jaan - E - Jigar Jaaneman",
    "maine pyar tumhi se" to "Maine Pyar Tumhi Se Kiya Hai",
    "dil cheer ke dekh" to "Dil Cheer Ke Dekh"
)

/**
 * Clean user search queries by stripping conversational/filler words
 */
fun cleanSearchQuery(raw: String): String {
    var q = raw.lowercase().trim()
    val filler = listOf("song", "songs", "mp3", "gaana", "gana", "video", "yrr", "yaar", "ki", "mile", "sunna", "bhai", "plz", "full", "audio")
    for (f in filler) {
        q = q.replace(Regex("\\b$f\\b"), " ").trim()
    }
    q = q.replace(Regex("\\s+"), " ").trim()
    return q
}

/**
 * Typo-Tolerant Fuzzy String Matching (Supports 1-2 mistyped characters)
 */
fun isFuzzyMatch(target: String, query: String): Boolean {
    val t = target.lowercase()
    val q = query.lowercase().trim()
    if (q.isEmpty()) return true
    if (t.contains(q)) return true

    val qWords = q.split(" ").filter { it.length > 1 }
    val tWords = t.split(" ").filter { it.length > 1 }
    for (qw in qWords) {
        for (tw in tWords) {
            if (tw.startsWith(qw) || qw.startsWith(tw)) return true
            if (levenshteinDistance(tw, qw) <= 2) return true
        }
    }
    return false
}

private fun levenshteinDistance(s1: String, s2: String): Int {
    val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
    for (i in 0..s1.length) dp[i][0] = i
    for (j in 0..s2.length) dp[0][j] = j
    for (i in 1..s1.length) {
        for (j in 1..s2.length) {
            val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
            dp[i][j] = minOf(
                dp[i - 1][j] + 1,
                dp[i][j - 1] + 1,
                dp[i - 1][j - 1] + cost
            )
        }
    }
    return dp[s1.length][s2.length]
}

/**
 * Decrypt JioSaavn DES-encrypted media stream URL to direct 160kbps MP4 audio.
 */
fun decryptSaavnMediaUrl(encryptedBase64: String): String {
    return try {
        if (encryptedBase64.isBlank()) return ""
        val keyBytes = "38346591".toByteArray(Charsets.UTF_8)
        val keySpec = javax.crypto.spec.SecretKeySpec(keyBytes, "DES")
        val cipher = javax.crypto.Cipher.getInstance("DES/ECB/PKCS5Padding")
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec)
        val decodedBytes = android.util.Base64.decode(encryptedBase64.trim(), android.util.Base64.DEFAULT)
        val decryptedBytes = cipher.doFinal(decodedBytes)
        var rawUrl = String(decryptedBytes, Charsets.UTF_8).trim()
        if (rawUrl.contains("_96.mp4")) rawUrl.replace("_96.mp4", "_160.mp4")
        else if (rawUrl.contains("_48.mp4")) rawUrl.replace("_48.mp4", "_160.mp4")
        else rawUrl
    } catch (_: Exception) { "" }
}

@Composable
fun AsyncThumbnail(
    url: String,
    modifier: Modifier = Modifier,
    placeholderIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(url) {
        if (url.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.connectTimeout = 6000
                    conn.readTimeout = 8000
                    conn.inputStream.use { input ->
                        bitmap = BitmapFactory.decodeStream(input)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(modifier = modifier.background(Color(0xFF27272A)), contentAlignment = Alignment.Center) {
            Icon(placeholderIcon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * Animated Sound Wave Visualizer
 * Vertical dancing bars reflecting real audio playback and song progress
 */
@Composable
fun AudioWaveVisualizer(
    progressPercent: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 28
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveAnim by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveAnim"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val isPlayed = (i.toFloat() / barCount) <= progressPercent
            val baseHeight = when (i % 5) {
                0 -> 12f
                1 -> 22f
                2 -> 30f
                3 -> 18f
                else -> 26f
            }
            val dynamicHeight = if (isPlaying) {
                val phase = ((i * 35) % 100) / 100f
                val factor = (waveAnim + phase) % 1f
                (baseHeight * (0.4f + 0.6f * factor)).coerceIn(6f, 32f)
            } else {
                (baseHeight * 0.5f).coerceIn(6f, 32f)
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(dynamicHeight.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isPlayed) Color(0xFF1DB954) else Color(0xFF3F3F46)
                    )
            )
        }
    }
}

/**
 * Spotify-Themed Full Music Hub Screen:
 * - Continuous Background Playback via Android ForegroundService (plays in pocket/screen off)
 * - Slim single-line search bar with live JioSaavn 1500+ song search & fuzzy matching
 * - Center Pop-up player with large cover, waves visualizer, and circle controls
 * - 90s Classics, Trending, Travel, Punjabi, Bhojpuri, Liked Songs & Offline tabs
 */
@Composable
fun MusicHubScreen(
    client: QwikSaveClient,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("90s") }

    // User favorites and downloaded files state
    var likedSongIds by remember { mutableStateOf(UserPreferences.getLikedSongIds(context)) }
    var downloadedPaths by remember { mutableStateOf(UserPreferences.getDownloadedSongPaths(context)) }

    // Dynamic songs list: local catalog + live JioSaavn fetched tracks
    val dynamicTracks = remember { mutableStateListOf<SongTrack>().apply { addAll(CURATED_SONGS_CATALOG) } }
    var isLiveSearching by remember { mutableStateOf(false) }

    // Center Pop-up / Now Playing Dialog State
    var showNowPlayingPopup by remember { mutableStateOf(false) }

    // Global Foreground Service State
    val currentTrack by MusicController.currentTrack.collectAsState()
    val isPlaying by MusicController.isPlaying.collectAsState()
    val progressSec by MusicController.progressSec.collectAsState()
    val durationSec by MusicController.durationSec.collectAsState()

    var isSeeking by remember { mutableStateOf(false) }
    var localSliderSec by remember { mutableStateOf(0f) }

    // Live Multi-Strategy Online Search Handler (Supports Titles, Lyrics & Autocomplete)
    var searchJob by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(searchQuery) {
        val raw = searchQuery.trim()
        val q = cleanSearchQuery(raw)
        if (q.length >= 2) {
            searchJob?.cancel()
            searchJob = coroutineScope.launch(Dispatchers.IO) {
                delay(300) // Debounce
                isLiveSearching = true
                try {
                    // Check alias first
                    var targetQuery = q
                    for ((k, v) in FAMOUS_LYRICS_ALIASES) {
                        if (raw.lowercase().contains(k) || q.contains(k)) {
                            targetQuery = v
                            break
                        }
                    }

                    val encoded = URLEncoder.encode(targetQuery, "UTF-8")
                    val searchUrl = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&api_version=4&ctx=web6dot0&p=1&n=25&q=$encoded"
                    val conn = (URL(searchUrl).openConnection() as HttpURLConnection).apply {
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        connectTimeout = 8000
                        readTimeout = 10000
                    }

                    var resultsCount = 0
                    if (conn.responseCode == 200) {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(body)
                        val results = json.optJSONArray("results") ?: json.optJSONObject("songs")?.optJSONArray("data")
                        if (results != null && results.length() > 0) {
                            resultsCount = results.length()
                            val fetched = mutableListOf<SongTrack>()
                            for (i in 0 until results.length()) {
                                val item = results.getJSONObject(i)
                                val songId = item.optString("id", "live_$i")
                                val songName = item.optString("title", item.optString("song", "Song"))
                                    .replace("&quot;", "\"")
                                    .replace("&amp;", "&")
                                val moreInfo = item.optJSONObject("more_info")
                                val artist = moreInfo?.optString("singers", moreInfo.optString("primary_artists", item.optString("description", "Bollywood Artist")))
                                    ?.replace("&quot;", "\"")
                                    ?.replace("&amp;", "&") ?: "Bollywood Artist"
                                val album = moreInfo?.optString("album", item.optString("album", "Bollywood Hits"))
                                    ?.replace("&quot;", "\"")
                                    ?.replace("&amp;", "&") ?: "Bollywood Hits"
                                var img = item.optString("image", "")
                                if (img.contains("150x150")) img = img.replace("150x150", "500x500")
                                else if (img.contains("50x50")) img = img.replace("50x50", "500x500")

                                val encUrl = moreInfo?.optString("encrypted_media_url", "") ?: ""
                                val decryptedUrl = if (encUrl.isNotBlank()) decryptSaavnMediaUrl(encUrl) else ""
                                val stream = if (decryptedUrl.isNotBlank() && decryptedUrl.startsWith("http")) decryptedUrl else item.optString("media_preview_url", "https://aac.saavncdn.com/588/1915cd0934f79eeb646ffebde384e59d_sar_160.mp4")

                                val durSec = moreInfo?.optString("duration", "210")?.toIntOrNull() ?: 210
                                val m = durSec / 60
                                val s = durSec % 60
                                val durText = String.format("%d:%02d", m, s)

                                fetched.add(
                                    SongTrack(
                                        id = songId,
                                        title = songName,
                                        artist = artist,
                                        albumOrMovie = album,
                                        durationText = durText,
                                        durationSec = durSec,
                                        streamUrl = stream,
                                        coverUrl = img.ifBlank { "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400&q=80" },
                                        category = "Trending"
                                    )
                                )
                            }
                            withContext(Dispatchers.Main) {
                                fetched.forEach { f ->
                                    if (dynamicTracks.none { it.id == f.id }) {
                                        dynamicTracks.add(0, f)
                                    }
                                }
                            }
                        }
                    }

                    // Fallback to autocomplete if search.getResults returned 0
                    if (resultsCount == 0) {
                        val autoUrl = "https://www.jiosaavn.com/api.php?__call=autocomplete.get&query=$encoded&_format=json&_marker=0&ctx=web6dot0"
                        val autoConn = (URL(autoUrl).openConnection() as HttpURLConnection).apply {
                            setRequestProperty("User-Agent", "Mozilla/5.0")
                            connectTimeout = 6000
                            readTimeout = 8000
                        }
                        if (autoConn.responseCode == 200) {
                            val autoBody = autoConn.inputStream.bufferedReader().use { it.readText() }
                            val autoJson = JSONObject(autoBody)
                            val songObj = autoJson.optJSONObject("songs")?.optJSONArray("data")
                            val topObj = autoJson.optJSONObject("topquery")?.optJSONArray("data")
                            val pid = songObj?.optJSONObject(0)?.optString("id")
                                ?: topObj?.optJSONObject(0)?.optString("id")
                            if (!pid.isNullOrBlank()) {
                                val detailUrl = "https://www.jiosaavn.com/api.php?__call=song.getDetails&pids=$pid&_format=json&_marker=0&ctx=web6dot0"
                                val detailConn = (URL(detailUrl).openConnection() as HttpURLConnection).apply {
                                    setRequestProperty("User-Agent", "Mozilla/5.0")
                                    connectTimeout = 6000
                                    readTimeout = 8000
                                }
                                if (detailConn.responseCode == 200) {
                                    val dBody = detailConn.inputStream.bufferedReader().use { it.readText() }
                                    val dJson = JSONObject(dBody)
                                    val songsArr = dJson.optJSONArray("songs")
                                    val sObj = if (songsArr != null && songsArr.length() > 0) songsArr.getJSONObject(0) else dJson.optJSONObject(pid)
                                    if (sObj != null) {
                                        val sTitle = sObj.optString("song", sObj.optString("title", targetQuery))
                                        val sArtist = sObj.optString("primary_artists", sObj.optString("singers", "Bollywood Artist"))
                                        val sAlbum = sObj.optString("album", "Bollywood Hits")
                                        val enc = sObj.optString("encrypted_media_url", "")
                                        val dec = if (enc.isNotBlank()) decryptSaavnMediaUrl(enc) else sObj.optString("media_preview_url", "")
                                        var im = sObj.optString("image", "")
                                        if (im.contains("150x150")) im = im.replace("150x150", "500x500")
                                        val dSec = sObj.optString("duration", "240").toIntOrNull() ?: 240

                                        val newTrack = SongTrack(
                                            id = pid,
                                            title = sTitle,
                                            artist = sArtist,
                                            albumOrMovie = sAlbum,
                                            durationText = String.format("%d:%02d", dSec / 60, dSec % 60),
                                            durationSec = dSec,
                                            streamUrl = dec.ifBlank { "https://aac.saavncdn.com/588/1915cd0934f79eeb646ffebde384e59d_sar_160.mp4" },
                                            coverUrl = im.ifBlank { "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400&q=80" },
                                            category = "Trending"
                                        )
                                        withContext(Dispatchers.Main) {
                                            if (dynamicTracks.none { it.id == newTrack.id }) {
                                                dynamicTracks.add(0, newTrack)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    isLiveSearching = false
                }
            }
        }
    }

    // Filtered song list
    val displayedTracks by remember(selectedCategory, searchQuery, dynamicTracks.size, likedSongIds, downloadedPaths) {
        derivedStateOf {
            val raw = searchQuery.trim().lowercase()
            val q = cleanSearchQuery(raw)
            dynamicTracks.filter { track ->
                val matchesCategory = when (selectedCategory) {
                    "90s" -> track.category == "90s"
                    "Trending" -> track.category == "Trending"
                    "Travel" -> track.category == "Travel"
                    "Punjabi" -> track.category == "Punjabi"
                    "Bhojpuri" -> track.category == "Bhojpuri"
                    "Liked" -> likedSongIds.contains(track.id)
                    "Offline" -> downloadedPaths.containsKey(track.id)
                    else -> true
                }
                val matchesSearch = raw.isEmpty() ||
                        isFuzzyMatch(track.title, raw) ||
                        isFuzzyMatch(track.title, q) ||
                        isFuzzyMatch(track.artist, raw) ||
                        isFuzzyMatch(track.artist, q) ||
                        isFuzzyMatch(track.albumOrMovie, raw)
                matchesCategory && matchesSearch
            }
        }
    }

    fun playTrack(track: SongTrack) {
        MusicController.playTrack(context, track, displayedTracks)
        showNowPlayingPopup = true // Automatically open center pop-up player on song tap
    }

    fun formatTime(totalSec: Int): String {
        val s = totalSec.coerceAtLeast(0)
        val m = s / 60
        val rem = s % 60
        return String.format("%02d:%02d", m, rem)
    }

    // Spotify Theme Styling (#121212 dark, #1DB954 vibrant Spotify green)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (currentTrack != null) 92.dp else 8.dp)
        ) {
            // Sleek Spotify Header with Small Compact Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF064E3B), Color(0xFF121212))
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Small Compact Back Button as Requested by User
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(26.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Qwik Music Hub", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1DB954))
                                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
                            ) {
                                Text("SPOTIFY VIBE 🎧", fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = Color.Black)
                            }
                        }
                        Text("All 70s, 80s, 90s, 2000s & Any Search • Ad-Free", fontSize = 9.5.sp, color = Color(0xFFB3B3B3))
                    }
                }

                if (isLiveSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF1DB954), strokeWidth = 2.dp)
                }
            }

            // Slim Single-Line Search Bar strictly in One Line
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search songs, lyrics, 70s-90s classics...", fontSize = 11.5.sp, color = Color(0xFF71717A), maxLines = 1) },
                singleLine = true,
                maxLines = 1,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF1DB954), modifier = Modifier.size(17.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(14.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1DB954),
                    unfocusedBorderColor = Color(0xFF27272A),
                    focusedContainerColor = Color(0xFF18181B),
                    unfocusedContainerColor = Color(0xFF18181B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            // Category Filter Chips
            val categories = listOf(
                Pair("90s", "📻 70s-90s Classics"),
                Pair("Trending", "🔥 Trending Hits"),
                Pair("Liked", "❤️ Liked (${likedSongIds.size})"),
                Pair("Offline", "💾 Saved (${downloadedPaths.size})"),
                Pair("All", "🌐 All Songs")
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(categories) { _, (key, label) ->
                    val isSelected = selectedCategory == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFF1DB954) else Color(0xFF242424))
                            .clickable { selectedCategory = key }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else Color.White
                        )
                    }
                }
            }

            // Song List
            if (displayedTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MusicOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Searching all songs & archives...", color = Color.Gray, fontSize = 12.sp)
                        if (searchQuery.isNotBlank()) {
                            Text("Fetching from live database...", color = Color(0xFF1DB954), fontSize = 11.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(displayedTracks) { index, track ->
                        val isCurrent = currentTrack?.id == track.id
                        val isLiked = likedSongIds.contains(track.id)
                        val isOffline = downloadedPaths.containsKey(track.id)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCurrent) Color(0xFF282828) else Color(0xFF181818))
                                .clickable { playTrack(track) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cover Thumbnail with Play overlay
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncThumbnail(
                                    url = track.coverUrl,
                                    modifier = Modifier.fillMaxSize(),
                                    placeholderIcon = Icons.Default.MusicNote
                                )
                                if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (isPlaying) Icons.Default.VolumeUp else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color(0xFF1DB954),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Title, Artist & Offline Indicator
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = track.title,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) Color(0xFF1DB954) else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (isOffline) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.DownloadDone,
                                            contentDescription = "Saved Offline",
                                            tint = Color(0xFF1DB954),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${track.artist} • ${track.albumOrMovie}",
                                    fontSize = 10.sp,
                                    color = Color(0xFFA1A1AA),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Like Heart Button
                            IconButton(
                                onClick = {
                                    val nowLiked = UserPreferences.toggleLikedSong(context, track.id)
                                    likedSongIds = UserPreferences.getLikedSongIds(context)
                                    val msg = if (nowLiked) "Added to Liked Songs ❤️" else "Removed from Liked Songs"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (isLiked) Color(0xFF1DB954) else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Offline Download Button
                            IconButton(
                                onClick = {
                                    if (isOffline) {
                                        Toast.makeText(context, "Already saved for offline playback! 💾", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Saving \"${track.title}\" for offline...", Toast.LENGTH_SHORT).show()
                                        coroutineScope.launch(Dispatchers.IO) {
                                            try {
                                                val cacheDir = File(context.cacheDir, "music_offline").apply { mkdirs() }
                                                val localFile = File(cacheDir, "${track.id}.mp3")
                                                val conn = (URL(track.streamUrl).openConnection() as HttpURLConnection).apply {
                                                    connectTimeout = 15000
                                                    readTimeout = 30000
                                                }
                                                if (conn.responseCode in 200..299) {
                                                    conn.inputStream.use { input ->
                                                        FileOutputStream(localFile).use { output ->
                                                            input.copyTo(output)
                                                        }
                                                    }
                                                    UserPreferences.saveDownloadedSongPath(context, track.id, localFile.absolutePath)
                                                    withContext(Dispatchers.Main) {
                                                        downloadedPaths = UserPreferences.getDownloadedSongPaths(context)
                                                        Toast.makeText(context, "Saved offline! Play anytime without internet 🎧", Toast.LENGTH_LONG).show()
                                                    }
                                                } else {
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(context, "Failed to download stream.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    if (isOffline) Icons.Default.CheckCircle else Icons.Default.Download,
                                    contentDescription = "Save Offline",
                                    tint = if (isOffline) Color(0xFF1DB954) else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sleek Spotify Floating Bottom Mini Player Bar
        if (currentTrack != null) {
            val track = currentTrack!!
            val isLiked = likedSongIds.contains(track.id)

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .clickable { showNowPlayingPopup = true }, // Click opens Center Pop-up!
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF202020)),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Song Cover & Info (Tap anywhere to open pop-up)
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black)
                            ) {
                                AsyncThumbnail(
                                    url = track.coverUrl,
                                    modifier = Modifier.fillMaxSize(),
                                    placeholderIcon = Icons.Default.MusicNote
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${track.artist} • ${track.albumOrMovie}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 9.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Playback Controls (Previous, Small Circle Play/Pause, Next)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = { MusicController.prevTrack(context) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(18.dp))
                            }

                            // Small Circular Spotify Play/Pause Button as Requested
                            IconButton(
                                onClick = { MusicController.togglePlayPause(context) },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0xFF1DB954), CircleShape)
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            IconButton(
                                onClick = { MusicController.nextTrack(context) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = {
                                    val nowLiked = UserPreferences.toggleLikedSong(context, track.id)
                                    likedSongIds = UserPreferences.getLikedSongIds(context)
                                    val msg = if (nowLiked) "Added to Liked Songs ❤️" else "Removed from Liked Songs"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (isLiked) Color(0xFF1DB954) else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Draggable Seekbar
                    val maxDur = durationSec.coerceAtLeast(1)
                    val currentVal = if (isSeeking) localSliderSec else progressSec.toFloat()

                    Slider(
                        value = currentVal.coerceIn(0f, maxDur.toFloat()),
                        valueRange = 0f..maxDur.toFloat(),
                        onValueChange = { newVal ->
                            isSeeking = true
                            localSliderSec = newVal
                        },
                        onValueChangeFinished = {
                            isSeeking = false
                            MusicController.seekTo(context, localSliderSec.toInt())
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFF1DB954),
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )

                    // Timing indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime(if (isSeeking) localSliderSec.toInt() else progressSec), color = Color(0xFF94A3B8), fontSize = 9.sp)
                        Text(formatTime(durationSec), color = Color(0xFF94A3B8), fontSize = 9.sp)
                    }
                }
            }
        }

        // CENTER POP-UP / NOW PLAYING MODAL CARD WITH WAVES AUDIO PROGRESS
        // Requested by user: "song wale me jo gana bajaye wo bich me pop up ki tarah bhi ban jaye like song ki pic jo rhti hai wo upar then niche chalte hue line ki kitna ho gaya and usme waves type me dikhe"
        if (showNowPlayingPopup && currentTrack != null) {
            val track = currentTrack!!
            val isLiked = likedSongIds.contains(track.id)
            val isOffline = downloadedPaths.containsKey(track.id)
            val maxDur = durationSec.coerceAtLeast(1)
            val currentVal = if (isSeeking) localSliderSec else progressSec.toFloat()
            val progressRatio = (currentVal / maxDur.toFloat()).coerceIn(0f, 1f)

            Dialog(
                onDismissRequest = { showNowPlayingPopup = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.82f))
                        .clickable { showNowPlayingPopup = false },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .wrapContentHeight()
                            .clickable(enabled = false) {} // Prevent dismiss on card click
                            .border(1.dp, Color(0xFF1DB954).copy(alpha = 0.35f), RoundedCornerShape(22.dp)),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
                        elevation = CardDefaults.cardElevation(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top Bar with Minimize Chevron & Title
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = { showNowPlayingPopup = false },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(Color(0xFF27272A), CircleShape)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize", tint = Color.White, modifier = Modifier.size(20.dp))
                                }

                                Text(
                                    text = "PLAYING NOW 🎧",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1DB954),
                                    letterSpacing = 1.sp
                                )

                                IconButton(
                                    onClick = {
                                        val nowLiked = UserPreferences.toggleLikedSong(context, track.id)
                                        likedSongIds = UserPreferences.getLikedSongIds(context)
                                        val msg = if (nowLiked) "Added to Liked Songs ❤️" else "Removed from Liked Songs"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(Color(0xFF27272A), CircleShape)
                                ) {
                                    Icon(
                                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Like",
                                        tint = if (isLiked) Color(0xFF1DB954) else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Large Center Song Pic Artwork
                            Box(
                                modifier = Modifier
                                    .size(210.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black)
                                    .border(1.dp, Color(0xFF27272A), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncThumbnail(
                                    url = track.coverUrl,
                                    modifier = Modifier.fillMaxSize(),
                                    placeholderIcon = Icons.Default.MusicNote
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Song Title & Artist / Album
                            Text(
                                text = track.title,
                                color = Color.White,
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${track.artist} • ${track.albumOrMovie}",
                                color = Color(0xFFA1A1AA),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // ANIMATED WAVES AUDIO PROGRESS VISUALIZER
                            // Multi-bar frequency waves that dance dynamically when playing, colored up to song progress!
                            AudioWaveVisualizer(
                                progressPercent = progressRatio,
                                isPlaying = isPlaying,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Interactive Draggable Seeker
                            Slider(
                                value = currentVal.coerceIn(0f, maxDur.toFloat()),
                                valueRange = 0f..maxDur.toFloat(),
                                onValueChange = { newVal ->
                                    isSeeking = true
                                    localSliderSec = newVal
                                },
                                onValueChangeFinished = {
                                    isSeeking = false
                                    MusicController.seekTo(context, localSliderSec.toInt())
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(20.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFF1DB954),
                                    inactiveTrackColor = Color(0xFF334155)
                                )
                            )

                            // Timing Labels
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(formatTime(if (isSeeking) localSliderSec.toInt() else progressSec), color = Color(0xFF94A3B8), fontSize = 10.5.sp)
                                Text(formatTime(durationSec), color = Color(0xFF94A3B8), fontSize = 10.5.sp)
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Playback Control Buttons (Previous, Small Circle Play/Pause, Next, Save)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Save Offline
                                IconButton(
                                    onClick = {
                                        if (isOffline) {
                                            Toast.makeText(context, "Already saved offline! 💾", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Saving offline...", Toast.LENGTH_SHORT).show()
                                            coroutineScope.launch(Dispatchers.IO) {
                                                try {
                                                    val cacheDir = File(context.cacheDir, "music_offline").apply { mkdirs() }
                                                    val localFile = File(cacheDir, "${track.id}.mp3")
                                                    val conn = (URL(track.streamUrl).openConnection() as HttpURLConnection).apply {
                                                        connectTimeout = 15000
                                                        readTimeout = 30000
                                                    }
                                                    if (conn.responseCode in 200..299) {
                                                        conn.inputStream.use { input ->
                                                            FileOutputStream(localFile).use { output ->
                                                                input.copyTo(output)
                                                            }
                                                        }
                                                        UserPreferences.saveDownloadedSongPath(context, track.id, localFile.absolutePath)
                                                        withContext(Dispatchers.Main) {
                                                            downloadedPaths = UserPreferences.getDownloadedSongPaths(context)
                                                            Toast.makeText(context, "Saved offline! 🎧", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                } catch (_: Exception) {}
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        if (isOffline) Icons.Default.DownloadDone else Icons.Default.Download,
                                        contentDescription = "Offline",
                                        tint = if (isOffline) Color(0xFF1DB954) else Color(0xFFA1A1AA),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Skip Previous
                                IconButton(
                                    onClick = { MusicController.prevTrack(context) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(26.dp))
                                }

                                // Sleek Small Circle Play/Pause Button
                                IconButton(
                                    onClick = { MusicController.togglePlayPause(context) },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(Color(0xFF1DB954), CircleShape)
                                ) {
                                    Icon(
                                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Skip Next
                                IconButton(
                                    onClick = { MusicController.nextTrack(context) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(26.dp))
                                }

                                // Close Dialog
                                IconButton(
                                    onClick = { showNowPlayingPopup = false },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFA1A1AA), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
