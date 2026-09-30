package com.example.car_limit_assistant

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.view.View
import android.util.TypedValue
import android.widget.RemoteViews

class CarWidgetProvider : AppWidgetProvider() {

    // 🔥 覆写此方法以在第一现场拦截含有“肉弹热数据”的 Intent 包
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE == action) {
            val extras = intent.extras
            if (extras != null && extras.containsKey("intent_content")) {
                // 如果是用户在主界面点击保存触发的，直接提取 Intent 里的瞬时最新数据
                val content = extras.getString("intent_content") ?: "暂无监控车辆"
                val fontSizeStr = extras.getString("intent_font_size", "13.0")
                val showTitleStr = extras.getString("intent_show_title", "true")

                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)

                if (appWidgetIds != null && appWidgetIds.size > 0) {
                    this.updateCustomWidget(context, appWidgetManager, appWidgetIds, content, fontSizeStr, showTitleStr)
                    return // 拦截成功后直接更新并返回，彻底绕过恶心的文件多进程死锁
                }
            }
        }
        // 如果是系统自发更新（没有带 Intent 传参），走标准流程（会调用下面的 onUpdate）
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 系统自动刷新时的文件兜底读取逻辑
        val widgetData = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
        val content = widgetData.getString("flutter.widget_content", "暂无监控车辆") ?: "暂无监控车辆"
        val fontSizeStr = widgetData.getString("flutter.widget_font_size_str", "13.0") ?: "13.0"
        val showTitleStr = widgetData.getString("flutter.widget_show_title_str", "true") ?: "true"

        updateCustomWidget(context, appWidgetManager, appWidgetIds, content, fontSizeStr, showTitleStr)
    }

    // 提取出来的公用统一刷图函数
    private fun updateCustomWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        content: String,
        fontSizeStr: String,
        showTitleStr: String
    ) {
        // 🛠️ 核心修复：1. 先提前把点击跳转到 App 主界面的 PendingIntent 包装好
        val configIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        // 适配 Android 12+ 强限制，添加必要的 IMMUTABLE 标志位
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, configIntent, flags)

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.car_widget_layout)

            // =================🔥 核心修复：把原本封死的逻辑改成绑定跳转意图 =================
            views.setOnClickPendingIntent(R.id.widget_text_view, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_title_view, pendingIntent)
            // =======================================================================

            // 1. 文字
            views.setTextViewText(R.id.widget_text_view, content)

            // 2. 字号
            val fontSize = fontSizeStr.toFloatOrNull() ?: 13f
            views.setTextViewTextSize(R.id.widget_text_view, TypedValue.COMPLEX_UNIT_SP, fontSize)

            // 3. 标题开关
            if (showTitleStr == "true") {
                views.setViewVisibility(R.id.widget_title_view, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_title_view, View.GONE)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}