package com.numberniceic.ui.apersonnews


import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.numberniceic.R
import com.numberniceic.data.persons.DressColorCollection
import com.numberniceic.data.persons.OutfitMiracleColorSetsRequest
import com.numberniceic.databinding.FragmentClothcolor3dfBinding
import com.numberniceic.databinding.ItemClothDaySectionBinding
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import com.numberniceic.utils.OutfitApiColorMappingManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.numberniceic.utils.PersonContextManager
import com.numberniceic.utils.UserContextManager
import org.joda.time.DateTime
import java.util.*



class Clothcolor3df : DialogFragment() {

    private lateinit var binding: FragmentClothcolor3dfBinding
    private lateinit var clothcolor3dModel: Clothcolor3dModel
    
    // Map to store loaded colors: Offset -> (GoodColors, BadColors)
    private val dayDataMap = mutableMapOf<Int, Pair<ArrayList<ColorStateList>?, ArrayList<ColorStateList>?>>()

    companion object {
        fun newInstance(): Clothcolor3df {
            val args = Bundle()
            val fragment = Clothcolor3df()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        clothcolor3dModel = ViewModelProvider(this)[Clothcolor3dModel::class.java]
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_clothcolor3df, container, false)
        binding.apply {
            lifecycleOwner = this@Clothcolor3df
            binding.clothColor3dObs = clothcolor3dModel.clothColor3dObs
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.90).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Thread {
            OutfitApiColorMappingManager.syncDayPalettesFromServer(requireContext())
            activity?.runOnUiThread {
                loadDataForDay(1)
                loadDataForDay(2)
                loadDataForDay(3)
            }
        }.start()

        updateUI()



        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }
    
    private fun loadDataForDay(offset: Int) {
        val userx = UserContextManager.userX(this.context!!) ?: return
        val dt = DateTime().plusDays(offset)
        val dayOfWeekEng = ViewModelHelper.resolveOutfitCurrentDayEng(dt)

        val sHour = userx.sHour
        val birthday: DateTime = try {
            DateTime.parse(userx.birthDay)
        } catch (e: Exception) {
            Log.e("Clothcolor3df", "Invalid birthday format: ${userx.birthDay}", e)
            return
        }
        val dayBirth = birthday!!.dayOfWeek().getAsText(Locale.ENGLISH)
        val dayNumBirth = PersonContextManager.convertDayEngToNum(dayBirth)
        
        val dayBirthNumber = if (sHour <= 4) {
            if (dayNumBirth == 1) 7 else dayNumBirth - 1
        } else {
            dayNumBirth
        }

        val dayBirthEng = PersonContextManager.convertDayNumToEng(dayBirthNumber) ?: return
        val ageYang = PersonContextManager.ageYang(birthday.year, birthday.monthOfYear, birthday.dayOfMonth) ?: return

        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        val reqBody = OutfitMiracleColorSetsRequest(
            currentDayName = ViewModelHelper.dayEngToOutfitDayName(dayOfWeekEng),
            birthDayName = ViewModelHelper.dayEngToOutfitDayName(dayBirthEng),
            ageYears = ageYang
        )
        apiService.getOutfitMiracleColorSets(reqBody).enqueue(object : Callback<com.numberniceic.data.persons.OutfitMiracleColorSetsResponse> {
            override fun onResponse(
                call: Call<com.numberniceic.data.persons.OutfitMiracleColorSetsResponse>,
                response: Response<com.numberniceic.data.persons.OutfitMiracleColorSetsResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val payload = response.body()!!
                    val inauspiciousSets = payload.inauspiciousSets ?: emptyList()
                    val inauspiciousNumbers = inauspiciousSets.mapNotNull { it.number }.toSet()

                    val effectiveAuspicious = ViewModelHelper.resolveAuspiciousSetsWithKaliCheck(
                        payload.auspiciousSets,
                        inauspiciousNumbers
                    )
                    val dressD = ViewModelHelper.toDressColorCollection(
                        effectiveAuspicious,
                        requireContext(),
                        ViewModelHelper.DressDisplayMode.AUSPICIOUS
                    )
                    val dressR = ViewModelHelper.toDressColorCollection(
                        inauspiciousSets,
                        requireContext(),
                        ViewModelHelper.DressDisplayMode.INAUSPICIOUS
                    )
                    val goodColors = dressD?.let { clothcolor3dModel.getColorSortxD(it, requireContext()) }
                    val badColors = dressR?.let { clothcolor3dModel.getColorSortxR(it, requireContext()) }
                    updateMap(offset, goodColors, true)
                    updateMap(offset, badColors, false)
                } else {
                    Log.e("Clothcolor3df", "offset=$offset outfit HTTP ${response.code()} body=${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<com.numberniceic.data.persons.OutfitMiracleColorSetsResponse>, t: Throwable) {
                Log.e("Clothcolor3df", "offset=$offset outfit failure: ${t.message}", t)
            }
        })
    }

    private fun updateMap(offset: Int, colors: ArrayList<ColorStateList>?, isGood: Boolean) {
        val current = dayDataMap[offset] ?: Pair(null, null)
        dayDataMap[offset] = if (isGood) {
            current.copy(first = colors)
        } else {
            current.copy(second = colors)
        }
        
        updateUI()
    }

    private fun updateUI() {
        // Today section (now Tomorrow)
        setSectionChips(binding.day1Section, dayDataMap[1]?.first, dayDataMap[1]?.second)
        // Tomorrow section (now Day After Tomorrow)
        setSectionChips(binding.day2Section, dayDataMap[2]?.first, dayDataMap[2]?.second)
        // Day 3 section
        setSectionChips(binding.day3Section, dayDataMap[3]?.first, dayDataMap[3]?.second)
    }

    private fun setSectionChips(sectionBinding: ItemClothDaySectionBinding, goodColors: ArrayList<ColorStateList>?, badColors: ArrayList<ColorStateList>?) {
        renderDynamicColorChips(sectionBinding.chipContainerGood, goodColors)
        renderDynamicColorChips(sectionBinding.chipContainerBad, badColors)
    }

    private fun renderDynamicColorChips(container: LinearLayout, colors: ArrayList<ColorStateList>?) {
        container.removeAllViews()
        if (colors.isNullOrEmpty()) {
            container.visibility = View.GONE
            return
        }
        container.visibility = View.VISIBLE
        colors.forEachIndexed { index, colorStateList ->
            val dot = CardView(requireContext())
            val size = dpToPx(14f)
            val params = LinearLayout.LayoutParams(size, size)
            if (index > 0) {
                params.marginStart = dpToPx(3f)
            }
            dot.layoutParams = params
            dot.radius = dpToPx(7f).toFloat()
            dot.cardElevation = 0f
            dot.setCardBackgroundColor(colorStateList.defaultColor)
            container.addView(dot)
        }
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
