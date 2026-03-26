import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class Userx(
    @SerializedName(value="memberid", alternate=["memberId", "id", "ID", "member_id"]) var userId: String?,
    @SerializedName(value="realname") val realName: String?
)

fun main() {
    val u = Userx("14", "Tayap")
    val json = Gson().toJson(u)
    println(json)
    val u2 = Gson().fromJson(json, Userx::class.java)
    println(u2.userId)
}
