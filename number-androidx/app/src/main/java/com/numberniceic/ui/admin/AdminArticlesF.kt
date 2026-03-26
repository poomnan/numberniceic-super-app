package com.numberniceic.ui.admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.JsonObject
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.numberniceic.data.admin.Article
import com.numberniceic.data.admin.ServerMessage
import com.numberniceic.R
import com.numberniceic.adapters.AdminArticleAdapter

class AdminArticlesF : Fragment() {

    private lateinit var rvArticles: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var adapter: AdminArticleAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_admin_articles, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        rvArticles = view.findViewById(R.id.rv_articles)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        fabAdd = view.findViewById(R.id.fab_add_article)

        adapter = AdminArticleAdapter(emptyList(), 
            onEdit = { article -> openEditArticle(article) },
            onDelete = { article -> confirmDelete(article) }
        )
        
        rvArticles.layoutManager = LinearLayoutManager(context)
        rvArticles.adapter = adapter

        swipeRefresh.setOnRefreshListener { loadArticles() }
        fabAdd.setOnClickListener { openAddArticle() }

        loadArticles()
    }

    private fun loadArticles() {
        swipeRefresh.isRefreshing = true
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getAdminArticles().enqueue(object : Callback<List<Article>> {
            override fun onResponse(call: Call<List<Article>>, response: Response<List<Article>>) {
                swipeRefresh.isRefreshing = false
                if (response.isSuccessful && response.body() != null) {
                    adapter.updateData(response.body()!!)
                } else {
                    Toast.makeText(context, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Article>>, t: Throwable) {
                swipeRefresh.isRefreshing = false
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openAddArticle() {
        val fragment = AdminArticleEditF.newInstance(null)
        parentFragmentManager.beginTransaction()
            .replace(R.id.admin_container, fragment) // Adjust container ID if needed
            .addToBackStack(null)
            .commit()
    }

    private fun openEditArticle(article: Article) {
        val fragment = AdminArticleEditF.newInstance(article)
        parentFragmentManager.beginTransaction()
            .replace(R.id.admin_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun confirmDelete(article: Article) {
        AlertDialog.Builder(requireContext())
            .setTitle("ยืนยันการลบ")
            .setMessage("คุณแน่ใจหรือไม่ว่าต้องการลบเรื่อง '${article.title}'?")
            .setPositiveButton("ลบ") { _, _ ->
                deleteArticle(article)
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    private fun deleteArticle(article: Article) {
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        val json = JsonObject()
        json.addProperty("art_id", article.artId)

        apiService.deleteAdminArticle(json).enqueue(object : Callback<ServerMessage> {
            override fun onResponse(call: Call<ServerMessage>, response: Response<ServerMessage>) {
                if (response.isSuccessful) {
                    Toast.makeText(context, "ลบบทความเรียบร้อย", Toast.LENGTH_SHORT).show()
                    loadArticles()
                } else {
                    Toast.makeText(context, "ลบไม่ได้: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ServerMessage>, t: Throwable) {
                Toast.makeText(context, "ลบไม่ได้: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
