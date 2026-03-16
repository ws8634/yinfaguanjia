package com.example.elderlyguardian.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.elderlyguardian.data.Medication
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 用药记录数据持久化存储
 * 使用SharedPreferences保存用药记录列表
 */
class MedicationStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "medication_prefs"
        private const val KEY_MEDICATIONS = "medications"
    }

    /**
     * 保存用药记录列表
     */
    fun saveMedications(medications: List<Medication>) {
        val json = gson.toJson(medications)
        prefs.edit().putString(KEY_MEDICATIONS, json).apply()
    }

    /**
     * 加载用药记录列表
     */
    fun loadMedications(): List<Medication> {
        val json = prefs.getString(KEY_MEDICATIONS, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<Medication>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    /**
     * 添加单个用药记录
     */
    fun addMedication(medication: Medication) {
        val medications = loadMedications().toMutableList()
        medications.add(medication)
        saveMedications(medications)
    }

    /**
     * 更新用药记录
     */
    fun updateMedication(updatedMedication: Medication) {
        val medications = loadMedications().toMutableList()
        val index = medications.indexOfFirst { it.id == updatedMedication.id }
        if (index != -1) {
            medications[index] = updatedMedication
            saveMedications(medications)
        }
    }

    /**
     * 删除用药记录
     */
    fun deleteMedication(medicationId: String) {
        val medications = loadMedications().toMutableList()
        medications.removeAll { it.id == medicationId }
        saveMedications(medications)
    }

    /**
     * 清空所有用药记录
     */
    fun clearAll() {
        prefs.edit().remove(KEY_MEDICATIONS).apply()
    }
}