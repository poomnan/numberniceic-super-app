package com.numberniceic.ui.admin

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.numberniceic.R
import com.numberniceic.data.admin.SacredTemple
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.numberniceic.adapters.SacredTempleAdapter

class SacredTemplePickerAct_Draft : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private var adapter: SacredTempleAdapter? = null
    // MemberID will be passed from previous screen or we pick a user first?
    // Wait, BuddhaAssignAct searches for a user first, THEN picks a buddha.
    // So SacredTemplePickerAct should probably be handling user selection too?
    
    // Ah, wait. The user asked for "Increase menu 'Sacred Old Temples' to send noti to user".
    // Usually admin picks user first.
    // Let's check BuddhaAssignAct.
    
    // For now, let's assume SacredTemplePickerAct handles USER SEARCH + TEMPLE PICK.
    // Or maybe copy BuddhaAssignAct logic.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Re-use BuddhaAssign layout but change logic?
        // Actually, BuddhaAssignAct is the one that searches user. 
        // Then it probably opens a dialog or another activity to pick the buddha.
        
        // Let's make SacredTemplePickerAct act like BuddhaAssignAct:
        // 1. Search User (by phone/username)
        // 2. Determine user
        // 3. Show list of temples to assign.
        
        // I'll create a new layout for this: activity_sacred_temple_picker.xml
        setContentView(R.layout.activity_buddha_assign) // Re-use layout for searching user
        
        // Wait, if I reuse layout, IDs must match.
        // Let's create a specialized Activity that mimics BuddhaAssignAct but for Temples.
        
        // Actually, to save time and reduce errors, let's reuse BuddhaAssignAct logic 
        // by creating a new class `SacredTempleAssignAct` (renamed from Picker to reflect functionality better)
    }
}
