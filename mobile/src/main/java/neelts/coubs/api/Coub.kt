package neelts.coubs.api

data class Coub (
	val id : Int,
	val raw_video_title : String,
	val file_versions : FileVersions,
	val audio_file_url : String
)

data class Timeline (
	val page : Int,
	val per_page : Int,
	val total_pages : Int,
	val next : Int,
	val coubs : List<Coub>
)

data class FileVersions (
	val html5 : Html5
)

data class Html5 (
	val video : Video,
	val audio : Audio
)

data class Audio (
	val high : Media?,
	val med : Media?,
	val sample_duration : Double
)

data class Video (
	val high : Media?,
	val med : Media?
)

data class Media (
	val url : String,
	val size : Int
)

/*
val flag : Boolean,
val abuses : String,
val recoubs_by_users_channels : List<String>,
val favourite : Boolean,
val recoub : Boolean,
val like : Boolean,
val in_my_best2015 : Boolean,
val id : Int,
val type : String,
val permalink : String,
val title : String,
val visibility_type : String,
val original_visibility_type : String,
val channel_id : Int,
val created_at : String,
val updated_at : String,
val is_done : Boolean,
val views_count : Int,
val cotd : String,
val cotd_at : String,
val published : Boolean,
val published_at : String,
val reversed : Boolean,
val from_editor_v2 : Boolean,
val is_editable : Boolean,
val original_sound : Boolean,
val has_sound : Boolean,
val recoub_to : String,
val file_versions : FileVersions,
val audio_versions : AudioVersions,
val image_versions : Image_versions,
val first_frame_versions : First_frame_versions,
val dimensions : Dimensions,
val site_w_h : List<Int>,
val page_w_h : List<Int>,
val site_w_h_small : List<Int>,
val age_restricted : Boolean,
val age_restricted_by_admin : Boolean,
val not_safe_for_work : String,
val allow_reuse : Boolean,
val dont_crop : Boolean,
val banned : Boolean,
val global_safe : String,
val audio_file_url : String,
val external_download : External_download,
val application : String,
val channel : Channel,
val file : String,
val picture : String,
val timeline_picture : String,
val small_picture : String,
val sharing_picture : String,
val percent_done : Int,
val tags : List<Tags>,
val categories : List<Categories>,
val recoubs_count : Int,
val remixes_count : Int,
val likes_count : Int,
val raw_video_id : Int,
val uploaded_by_ios_app : Boolean,
val uploaded_by_android_app : Boolean,
val media_blocks : Media_blocks,
val raw_video_thumbnail_url : String,
val raw_video_title : String,
val video_block_banned : Boolean,
val duration : Double,
val promo_winner : Boolean,
val promo_winner_recoubers : String,
val editorial_info : Editorial_info,
val promo_hint : String,
val beeline_best_2014 : String,
val from_web_editor : Boolean,
val normalize_sound : Boolean,
val best2015_addable : Boolean,
val ahmad_promo : String,
val promo_data : String,
val audio_copyright_claim : String,
val ads_disabled : String,
val position_on_page : Int*/
