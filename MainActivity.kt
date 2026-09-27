package com.egen.bosstimer

import android.app.*
import android.os.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.view.*
import android.widget.*
import java.util.*
import org.json.JSONArray
import org.json.JSONObject

data class Boss(var id: Long, var name: String, var end: Long)

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("bosses", MODE_PRIVATE) }
    private val bosses = mutableListOf<Boss>()
    private lateinit var container: LinearLayout
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        load()
        if (bosses.isEmpty()) {
            bosses.add(Boss(1, "보스 1", System.currentTimeMillis()+3600000))
            bosses.add(Boss(2, "보스 2", System.currentTimeMillis()+7200000))
            save()
        }
        requestPermissionsIfNeeded()
        buildUi()
        scheduleAll()
        handler.post(object: Runnable {
            override fun run() { refresh(); handler.postDelayed(this,1000) }
        })
    }

    private fun requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 100)
        val am = getSystemService(ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            Toast.makeText(this,"설정에서 정확한 알람 권한을 허용하면 5분 전 알림이 정확해집니다.",Toast.LENGTH_LONG).show()
        }
    }

    private fun buildUi() {
        val root=LinearLayout(this); root.orientation=LinearLayout.VERTICAL; root.setPadding(24,24,24,18)
        val title=TextView(this); title.text="에겐 길드 보스 타이머"; title.textSize=22f; title.setTextColor(Color.BLACK); title.setTypeface(null,1)
        root.addView(title, LinearLayout.LayoutParams(-1,-2))
        val add=Button(this); add.text="+ 보스 추가"; add.setOnClickListener{ addBoss() }
        root.addView(add)
        val scroll=ScrollView(this); container=LinearLayout(this); container.orientation=LinearLayout.VERTICAL; scroll.addView(container)
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
    }

    private fun refresh() {
        for(i in 0 until container.childCount) {
            val card=container.getChildAt(i) as? LinearLayout ?: continue
            val id=card.tag as? Long ?: continue
            val b=bosses.find{it.id==id} ?: continue
            val tv=card.findViewWithTag<TextView>("time")
            tv?.text=format(b.end-System.currentTimeMillis())
        }
    }

    private fun redraw() {
        container.removeAllViews()
        for(b in bosses) {
            val card=LinearLayout(this); card.orientation=LinearLayout.VERTICAL; card.setPadding(16,12,16,12); card.tag=b.id
            val top=LinearLayout(this); top.orientation=LinearLayout.HORIZONTAL
            val name=TextView(this); name.text=b.name; name.textSize=19f; name.setTypeface(null,1)
            top.addView(name,LinearLayout.LayoutParams(0,-2,1f))
            val edit=Button(this); edit.text="수정"; edit.setOnClickListener{ editBoss(b.id) }; top.addView(edit)
            card.addView(top)
            val time=TextView(this); time.tag="time"; time.textSize=30f; time.setTypeface(null,1); card.addView(time)
            val row=LinearLayout(this); row.orientation=LinearLayout.HORIZONTAL
            listOf("+10분" to 10L,"+30분" to 30L,"+1시간" to 60L).forEach{ (txt,min) ->
                val bt=Button(this); bt.text=txt; bt.setOnClickListener{ b.end += min*60000; save(); schedule(b); redraw() }
                row.addView(bt,LinearLayout.LayoutParams(0,-2,1f))
            }
            val reset=Button(this); reset.text="리셋"; reset.setOnClickListener{ editTime(b.id) }; row.addView(reset,LinearLayout.LayoutParams(0,-2,1f))
            card.addView(row)
            val del=Button(this); del.text="보스 삭제"; del.setOnClickListener{ bosses.removeIf{it.id==b.id}; save(); cancelAlarm(b.id); redraw() }; card.addView(del)
            container.addView(card)
        }
        refresh()
    }

    private fun editBoss(id:Long) {
        val b=bosses.find{it.id==id} ?: return
        val input=EditText(this); input.setText(b.name); input.hint="보스 이름"
        AlertDialog.Builder(this).setTitle("보스 이름 수정").setView(input)
            .setPositiveButton("저장"){_,_-> b.name=input.text.toString().ifBlank{"보스"}; save(); redraw()}
            .setNegativeButton("취소",null).show()
    }

    private fun editTime(id:Long) {
        val b=bosses.find{it.id==id} ?: return
        val input=EditText(this); input.inputType=2; input.hint="남은 시간(분)"
        input.setText(Math.max(0,((b.end-System.currentTimeMillis()+59999)/60000)).toString())
        AlertDialog.Builder(this).setTitle("${b.name} 시간 수정").setView(input)
            .setPositiveButton("저장"){_,_-> val m=input.text.toString().toLongOrNull()?:0; b.end=System.currentTimeMillis()+m*60000; save(); schedule(b); redraw()}
            .setNegativeButton("취소",null).show()
    }

    private fun addBoss() {
        val box=LinearLayout(this); box.orientation=LinearLayout.VERTICAL
        val name=EditText(this); name.hint="보스 이름"
        val min=EditText(this); min.hint="초기 시간(분)"; min.inputType=2; min.setText("60")
        box.addView(name); box.addView(min)
        AlertDialog.Builder(this).setTitle("보스 추가").setView(box)
            .setPositiveButton("추가"){_,_-> val b=Boss(System.currentTimeMillis(),name.text.toString().ifBlank{"새 보스"},System.currentTimeMillis()+(min.text.toString().toLongOrNull()?:60)*60000); bosses.add(b); save(); schedule(b); redraw()}
            .setNegativeButton("취소",null).show()
    }

    private fun format(ms:Long):String {
        if(ms<=0) return "출현!"
        var s=(ms+999)/1000; val h=s/3600; s%=3600; val m=s/60; val sec=s%60
        return "%02d:%02d:%02d".format(h,m,sec)
    }

    private fun save() {
        val a=JSONArray(); bosses.forEach{a.put(JSONObject().put("id",it.id).put("name",it.name).put("end",it.end))}
        prefs.edit().putString("data",a.toString()).apply()
    }
    private fun load() {
        try { val a=JSONArray(prefs.getString("data","[]")); for(i in 0 until a.length()){val o=a.getJSONObject(i); bosses.add(Boss(o.getLong("id"),o.getString("name"),o.getLong("end")))}} catch(_:Exception){}
    }
    private fun scheduleAll(){ bosses.forEach{schedule(it)} }
    private fun schedule(b:Boss){
        cancelAlarm(b.id)
        val trigger=b.end-5*60000
        if(trigger<=System.currentTimeMillis()) return
        val am=getSystemService(ALARM_SERVICE) as AlarmManager
        val pi=PendingIntent.getBroadcast(this,b.id.toInt(),Intent(this,AlarmReceiver::class.java).putExtra("name",b.name),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if(Build.VERSION.SDK_INT>=23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,trigger,pi) else am.setExact(AlarmManager.RTC_WAKEUP,trigger,pi)
    }
    private fun cancelAlarm(id:Long){
        val pi=PendingIntent.getBroadcast(this,id.toInt(),Intent(this,AlarmReceiver::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        (getSystemService(ALARM_SERVICE) as AlarmManager).cancel(pi)
    }
}