package com.numberniceic.ui.admin

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import java.io.IOException
import java.net.URI

class TopicModel:ViewModel() {

    private val topicObs = TopicObs()

    init {

    }


    fun getTopicId(): String? {
        return topicObs.topicId
    }


    fun setTopicId(topicId:String){
        topicObs.topicId = topicId
    }

    fun getTopicAuthName(): String? {
        return topicObs.authName
    }


    fun setTopicAuthName(authName:String){
        topicObs.authName = authName
    }

    fun setTopicDateTime(dateTime:String){
        topicObs.topicDateTime = dateTime
    }

    fun getTopicDateTime(): String? {
        return topicObs.topicDateTime
    }

    fun getBase64Photo3(): String? {
        return topicObs.imgBase64Photo3
    }

    fun setBase64Photo3(photoBase64: String){
        topicObs.imgBase64Photo3 = photoBase64
    }


    fun getBase64Photo2(): String? {
        return topicObs.imgBase64Photo2
    }

    fun setBase64Photo2(photoBase64Str:String){
        topicObs.imgBase64Photo2 = photoBase64Str
    }

    fun getBase64Photo1(): String? {
        return topicObs.imgBase64Photo1
    }

    fun setBase64Photo1(photoBase64Str:String){
        topicObs.imgBase64Photo1 = photoBase64Str
    }

    fun getTopicObs(): TopicObs {
        return topicObs
    }

    fun setTopicHeader(topic:String){
        topicObs.topicHeader = topic
    }

    fun getTopicHeader(): String {
        return topicObs.topicHeader
    }

    fun setTopicDesc(desc:String){
        topicObs.topicDesc = desc
    }

    fun getTopicDesc(): String {
        return topicObs.topicDesc
    }

    fun setParagraph1(paraText:String){
        topicObs.topicParagraph1 = paraText
    }

    fun getParagraph1(): String {
        return topicObs.topicParagraph1
    }


    fun setParagraph2(paraText:String){
        topicObs.topicParagraph2 = paraText
    }

    fun getParagraph2(): String {
        return topicObs.topicParagraph2
    }


    fun setParagraph3(paraText:String){
        topicObs.topicParagraph3 = paraText
    }

    fun getParagraph3(): String {
        return topicObs.topicParagraph3
    }





    fun setChipNameSur(hashTag:Boolean){
        topicObs.chipNameSur = hashTag
    }
    fun getChipNameSur():String{
        return topicObs.chipNameSur.toString()
    }


    fun setChipHome(hashTag:Boolean){
        topicObs.chipHome = hashTag
    }
    fun getChipHome():String{
        return topicObs.chipHome.toString()
    }


    fun setChipTabian(hashTag:Boolean){
        topicObs.chipTabian = hashTag
    }

    fun getChipTabian():String{
        return topicObs.chipTabian.toString()
    }



    fun setChipPhone(hashTag:Boolean){
        topicObs.chipPhone = hashTag
    }
    fun getChipPhone():String{
        return topicObs.chipPhone.toString()
    }




    fun setImgHeader(imgX: Uri?){
        topicObs.imgHeader = imgX
    }

    fun getImgHeader(): Uri? {
        return topicObs.imgHeader
    }

    fun setImgParagraph1(imgX: Uri?){
        topicObs.imgParagraph1 = imgX
    }

    fun getImgParagraph1(): Uri? {
        return topicObs.imgParagraph1
    }

    fun setImgParagraph2(imgX: Uri?){
        topicObs.imgParagraph2 = imgX
    }

    fun getImgParagraph2(): Uri? {
        return topicObs.imgParagraph2
    }




}