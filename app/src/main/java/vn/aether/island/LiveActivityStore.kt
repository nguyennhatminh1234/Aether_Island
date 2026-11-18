package vn.aether.island

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class LiveItem(val id:String,val title:String,val detail:String,val kind:String,val updated:Long)

object LiveActivityStore {
    const val ACTION_CHANGED = "vn.aether.island.ACTIVITIES_CHANGED"
    private const val PREFS = "live_activities"
    private const val KEY = "items"
    private const val TIMER_END = "timer_end"
    private const val STOPWATCH_START = "stopwatch_start"

    @Synchronized fun all(context:Context):List<LiveItem> {
        val raw=context.getSharedPreferences(PREFS,0).getString(KEY,"[]") ?: "[]"
        return try {
            val a=JSONArray(raw); (0 until a.length()).map { i ->
                val o=a.getJSONObject(i)
                LiveItem(o.getString("id"),o.getString("title"),o.optString("detail"),o.getString("kind"),o.optLong("updated"))
            }
        } catch (_:Exception) { emptyList() }
    }
    @Synchronized fun put(context:Context,item:LiveItem) {
        val items=all(context).filterNot { it.id==item.id }.toMutableList()
        items.add(0,item)
        val a=JSONArray()
        items.take(8).forEach { a.put(JSONObject().put("id",it.id).put("title",it.title).put("detail",it.detail).put("kind",it.kind).put("updated",it.updated)) }
        context.getSharedPreferences(PREFS,0).edit().putString(KEY,a.toString()).apply()
        changed(context)
    }
    @Synchronized fun remove(context:Context,id:String) {
        val a=JSONArray()
        all(context).filterNot { it.id==id }.forEach { a.put(JSONObject().put("id",it.id).put("title",it.title).put("detail",it.detail).put("kind",it.kind).put("updated",it.updated)) }
        context.getSharedPreferences(PREFS,0).edit().putString(KEY,a.toString()).apply()
        changed(context)
    }
    fun promote(context:Context,id:String) {
        val chosen=all(context).firstOrNull { it.id==id } ?: return
        put(context,chosen.copy(updated=System.currentTimeMillis()))
    }
    fun startTimer(context:Context,seconds:Int) {
        context.getSharedPreferences(PREFS,0).edit().putLong(TIMER_END,System.currentTimeMillis()+seconds*1000L).apply()
        put(context,LiveItem("timer","Timer","Started · " + seconds + " seconds","timer",System.currentTimeMillis()))
    }
    fun stopTimer(context:Context) { context.getSharedPreferences(PREFS,0).edit().remove(TIMER_END).apply(); remove(context,"timer") }
    fun timerEnd(context:Context)=context.getSharedPreferences(PREFS,0).getLong(TIMER_END,0L)
    fun startStopwatch(context:Context) {
        val now=System.currentTimeMillis()
        context.getSharedPreferences(PREFS,0).edit().putLong(STOPWATCH_START,now).apply()
        put(context,LiveItem("stopwatch","Stopwatch","Running","stopwatch",now))
    }
    fun stopStopwatch(context:Context) { context.getSharedPreferences(PREFS,0).edit().remove(STOPWATCH_START).apply(); remove(context,"stopwatch") }
    fun stopwatchStart(context:Context)=context.getSharedPreferences(PREFS,0).getLong(STOPWATCH_START,0L)
    fun changed(context:Context) { context.sendBroadcast(android.content.Intent(ACTION_CHANGED).setPackage(context.packageName)) }
}
