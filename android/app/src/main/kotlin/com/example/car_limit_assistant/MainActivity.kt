package com.example.car_limit_assistant

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.example.car_limit_assistant/widget"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "updateWidgetData") {
                val content = call.argument<String>("content") ?: "暂无监控车辆"
                val fontSize = call.argument<Double>("fontSize") ?: 13.0
                val showTitle = call.argument<Boolean>("showTitle") ?: true

                // 1. 保险起见，本地依然保存一份供小组件系统自动重启时加载
                val prefs = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putString("flutter.widget_content", content)
                    putString("flutter.widget_font_size_str", fontSize.toString())
                    putString("flutter.widget_show_title_str", showTitle.toString())
                    apply()
                }

                // 2. 🔥 终极绝杀：直接把最新数据当作“肉弹”打包塞进广播包，不给多进程缓存留任何机会
                val intent = Intent(context, CarWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra("intent_content", content)
                    putExtra("intent_font_size", fontSize.toString())
                    putExtra("intent_show_title", showTitle.toString())
                }

                val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                    ComponentName(context, CarWidgetProvider::class.java)
                )
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                context.sendBroadcast(intent)

                result.success(true)
            } else {
                result.notImplemented()
            }
        }
    }
}