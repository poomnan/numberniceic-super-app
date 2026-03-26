package com.numberniceic.ui.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.numberniceic.R
import com.numberniceic.utils.showSingle

class NotificationBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notification_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Mark all as read immediately when viewed
        com.numberniceic.data.local.NotificationStorage.markAllRead(requireContext())
        requireContext().sendBroadcast(android.content.Intent("com.numberniceic.NEW_NOTIFICATION"))
        
        // New logic: Also mark read when user clicks/acknowledges specific item.
        // This keeps the badge accurate to actual unread items.

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_notifications)
        recyclerView.layoutManager = LinearLayoutManager(context)

        fun loadList() {
            // Real Data
            val realItems = com.numberniceic.data.local.NotificationStorage.getNotifications(requireContext())
            val mappedItems = realItems.map { NotiItem(it.title, it.body, it.dateDisplay, it.type, it.url) }

            val displayItems = if (mappedItems.isNotEmpty()) {
                mappedItems
            } else {
                // Show empty state message
                listOf(NotiItem("ไม่มีการแจ้งเตือน", "คุณจะได้รับการแจ้งเตือนเกี่ยวกับสีกระเป๋ามงคล วันพระ และอื่นๆ ที่นี่", ""))
            }

            recyclerView.adapter = NotificationAdapter(displayItems, { item ->
                if (item.title == "ไม่มีการแจ้งเตือน") return@NotificationAdapter
                
                // 🔔 Mark as read immediately when clicked/acknowledged
                com.numberniceic.data.local.NotificationStorage.markReadByContent(requireContext(), item.title, item.body)
                requireContext().sendBroadcast(android.content.Intent("com.numberniceic.NEW_NOTIFICATION"))

                if (item.type == "bag_color" || item.title.contains("สีกระเป๋า") || item.body.contains("สีกระเป๋า")) {
                    dismissAllowingStateLoss()
                    
                    val act = activity
                    if (act is com.numberniceic.ui.AppActivity) {
                        act.showDashboard(forceRefresh = true)
                    } else {
                        val dashboardSheet = com.numberniceic.ui.apersonnews.DashboardBottomSheet()
                        val args = android.os.Bundle()
                        args.putBoolean("force_refresh", true)
                        dashboardSheet.arguments = args
                        dashboardSheet.showSingle(parentFragmentManager, "DashboardBottomSheet")
                    }
                } else {
                    // Show special notification popup for other types
                    val specialBS = SpecialNotificationDialogFragment.newInstance(item.title, item.body, item.url)
                    specialBS.showSingle(parentFragmentManager, "SpecialNotificationBottomSheet")
                }
            }, { position ->
                if (mappedItems.isNotEmpty()) {
                    com.numberniceic.data.local.NotificationStorage.deleteNotificationAt(requireContext(), position)
                    android.widget.Toast.makeText(context, "ลบรายการเรียบร้อย", android.widget.Toast.LENGTH_SHORT).show()
                    loadList()
                    // Broadcast to update badge
                    requireContext().sendBroadcast(android.content.Intent("com.numberniceic.NEW_NOTIFICATION"))
                } else {
                     android.widget.Toast.makeText(context, "ไม่มีรายการให้ลบ", android.widget.Toast.LENGTH_SHORT).show()
                }
            })
        }


        // Swipe to Delete
        val swipeHandler = object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.LEFT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val currentItems = com.numberniceic.data.local.NotificationStorage.getNotifications(requireContext())
                
                if (currentItems.isNotEmpty() && position >= 0 && position < currentItems.size) {
                    com.numberniceic.data.local.NotificationStorage.deleteNotificationAt(requireContext(), position)
                    
                    // Show SnackBar or Toast with Undo? Toast for now based on request.
                    android.widget.Toast.makeText(context, "ลบรายการแล้ว", android.widget.Toast.LENGTH_SHORT).show()
                    
                    loadList() // Refresh List
                    
                    // Update Badge
                    requireContext().sendBroadcast(android.content.Intent("com.numberniceic.NEW_NOTIFICATION"))
                } else {
                    // If empty state or invalid, refresh to restore item view
                    loadList()
                }
            }

            override fun onChildDraw(c: android.graphics.Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                                    dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                
                // Draw Background Red with Trash Icon
                if (actionState == androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                    val itemView = viewHolder.itemView
                    val paint = android.graphics.Paint()
                    paint.color = android.graphics.Color.parseColor("#F44336") // Red
                    
                    val background = android.graphics.RectF(
                        itemView.right.toFloat() + dX,
                        itemView.top.toFloat(),
                        itemView.right.toFloat(),
                        itemView.bottom.toFloat()
                    )
                    c.drawRect(background, paint)

                    // Optional: Draw Trash Icon (if you want to be extra fancy)
                    val icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete_vector)
                    if (icon != null) {
                        icon.setTint(android.graphics.Color.WHITE)
                        val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = iconTop + icon.intrinsicHeight
                        val iconRight = itemView.right - iconMargin
                        val iconLeft = iconRight - icon.intrinsicWidth
                        
                        // Only draw icon if swiped enough
                        if (dX < -iconMargin) {
                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                            icon.draw(c)
                        }
                    }
                }
                
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
            
            // Disable swipe for empty state item
            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                 val currentItems = com.numberniceic.data.local.NotificationStorage.getNotifications(requireContext())
                 if (currentItems.isEmpty()) return 0
                 return super.getSwipeDirs(recyclerView, viewHolder)
            }
        }
        
        val itemTouchHelper = androidx.recyclerview.widget.ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        loadList()
    }    override fun onStart() {
        super.onStart()
        val dialog = dialog as? com.google.android.material.bottomsheet.BottomSheetDialog
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let { sheet ->
            val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet)
            val displayMetrics = resources.displayMetrics
            val totalHeight = displayMetrics.heightPixels
            val targetHeight = (totalHeight * 0.95).toInt()

            sheet.layoutParams.height = targetHeight
            behavior.isFitToContents = false
            behavior.expandedOffset = totalHeight - targetHeight
            behavior.peekHeight = targetHeight
            behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        }
    }
}
