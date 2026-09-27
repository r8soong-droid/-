package com.egen.bosstimer
import android.content.*
import org.json.JSONArray
import android.app.*
import android.os.Build

class BootReceiver: BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        val p=context.getSharedPreferences("bosses",Context.MODE_PRIVATE)
        try{
            val a=JSONArray(p.getString("data","[]"))
            val am=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            for(i in 0 until a.length()){
                val o=a.getJSONObject(i); val id=o.getLong("id"); val end=o.getLong("end"); val trigger=end-300000
                if(trigger<=System.currentTimeMillis()) continue
                val pi=PendingIntent.getBroadcast(context,id.toInt(),Intent(context,AlarmReceiver::class.java).putExtra("name",o.getString("name")),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                if(Build.VERSION.SDK_INT>=23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,trigger,pi) else am.setExact(AlarmManager.RTC_WAKEUP,trigger,pi)
            }
        }catch(_:Exception){}
    }
}