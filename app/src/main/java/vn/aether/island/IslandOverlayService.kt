package vn.aether.island

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.animation.ValueAnimator
import android.view.animation.OvershootInterpolator
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs
import kotlin.math.min

class IslandOverlayService : Service() {
    private enum class IslandState { HIDE, COMPACT, EXPANDED }
    companion object {
        private const val CHANNEL_ID = "aether_island_overlay"
        private const val NOTIFICATION_ID = 101
        const val ACTION_STOP = "vn.aether.island.STOP"
    }
    private lateinit var windowManager: WindowManager
    private var islandView: IslandView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val channel = NotificationChannel(CHANNEL_ID,"Aether Island",NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        startForeground(NOTIFICATION_ID, Notification.Builder(this,CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle("Aether Island is active")
            .setContentText("Live activities are running").setOngoing(true).build())
    }
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(intent?.action==ACTION_STOP){stopIsland();stopSelf();return START_NOT_STICKY}
        if(!Settings.canDrawOverlays(this)){stopSelf();return START_NOT_STICKY}
        if(islandView==null){
            val view=IslandView();islandView=view
            val params=WindowManager.LayoutParams(view.widthPx,view.currentWindowHeight,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                android.graphics.PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.CENTER_HORIZONTAL;x=0;y=0}
            layoutParams=params
            try{windowManager.addView(view,params);view.animateEntrance()}catch(_:Exception){islandView=null;layoutParams=null;stopSelf()}
        }
        return START_NOT_STICKY
    }
    private fun updateOverlaySize(width:Int,height:Int){
        val view=islandView?:return;val params=layoutParams?:return
        if(params.width==width&&params.height==height)return
        params.width=width;params.height=height
        try{windowManager.updateViewLayout(view,params)}catch(_:Exception){stopIsland();stopSelf()}
    }
    private fun stopIsland(){islandView?.let{try{windowManager.removeView(it)}catch(_:Exception){}};islandView=null;layoutParams=null}
    override fun onDestroy(){stopIsland();super.onDestroy()}
    override fun onBind(intent:Intent?):IBinder?=null

    private inner class IslandView:View(this@IslandOverlayService){
        private val density=resources.displayMetrics.density
        private fun dp(v:Float)=v*density
        private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        private val bounds=RectF()
        private var state=IslandState.HIDE
        private var animatedWidth=dp(42f);private var animatedHeight=dp(7f);private var animatedRadius=dp(4f)
        private var downX=0f;private var downY=0f;private var moved=false
        private var animator:ValueAnimator?=null
        private var stackIndex=0
        private val refresh=object:Runnable{override fun run(){invalidate();postDelayed(this,1000L)}}
        val widthPx get()=dp(48f).toInt()
        val currentWindowHeight get()=dp(12f).toInt()
        init{setLayerType(View.LAYER_TYPE_HARDWARE,null);isClickable=true;post(refresh)}
        fun animateEntrance(){animateTo(IslandState.COMPACT,520L,true);postDelayed({if(state==IslandState.COMPACT)animateTo(IslandState.HIDE)},2600L)}
        private fun animateTo(target:IslandState,duration:Long=360L,entrance:Boolean=false){
            animator?.cancel();state=target
            this@IslandOverlayService.layoutParams?.let{p->val y=if(target==IslandState.HIDE)-dp(5f).toInt() else 0;if(p.y!=y){p.y=y;try{windowManager.updateViewLayout(this,p)}catch(_:Exception){}}}
            val sw=animatedWidth;val sh=animatedHeight;val sr=animatedRadius
            val ew=when(target){
                IslandState.HIDE->dp(42f)
                IslandState.COMPACT->dp(132f)
                IslandState.EXPANDED->resources.displayMetrics.widthPixels.toFloat()
            }
            val eh=when(target){IslandState.HIDE->dp(7f);IslandState.COMPACT->dp(38f);IslandState.EXPANDED->dp(142f)}
            val er=when(target){IslandState.HIDE->dp(4f);IslandState.COMPACT->dp(19f);IslandState.EXPANDED->dp(35f)}
            animator=ValueAnimator.ofFloat(0f,1f).apply{this.duration=duration;interpolator=if(target==IslandState.EXPANDED||entrance)OvershootInterpolator(1.15f)else DecelerateInterpolator()
                addUpdateListener{val f=it.animatedValue as Float;animatedWidth=min(sw+(ew-sw)*f,ew)
                    animatedHeight=sh+(eh-sh)*f
                    animatedRadius=sr+(er-sr)*f
                    val windowWidth=if(target==IslandState.EXPANDED) resources.displayMetrics.widthPixels else (animatedWidth+dp(6f)).toInt()
                    updateOverlaySize(windowWidth,(animatedHeight+dp(4f)).toInt());invalidate()};start()}
        }
        private fun activeItems():List<LiveItem>{
            val items=LiveActivityStore.all(this@IslandOverlayService)
            val end=LiveActivityStore.timerEnd(this@IslandOverlayService)
            if(end>0L){val left=((end-System.currentTimeMillis()).coerceAtLeast(0L)+999)/1000
                if(left==0L)LiveActivityStore.stopTimer(this@IslandOverlayService)
                else LiveActivityStore.put(this@IslandOverlayService,LiveItem("timer","Timer","%02d:%02d".format(left/60,left%60),"timer",end))}
            val start=LiveActivityStore.stopwatchStart(this@IslandOverlayService)
            if(start>0L){val sec=(System.currentTimeMillis()-start)/1000;LiveActivityStore.put(this@IslandOverlayService,LiveItem("stopwatch","Stopwatch","%02d:%02d".format(sec/60,sec%60),"stopwatch",start))}
            return LiveActivityStore.all(this@IslandOverlayService)
        }
        override fun onDraw(canvas:Canvas){
            super.onDraw(canvas);val left=width/2f-animatedWidth/2f;val top=dp(2f)
            bounds.set(left,top,left+animatedWidth,top+animatedHeight);paint.color=android.graphics.Color.BLACK;paint.style=Paint.Style.FILL
            canvas.drawRoundRect(bounds,animatedRadius,animatedRadius,paint)
            if(state==IslandState.HIDE||animatedHeight<dp(20f))return
            val items=activeItems();val selected=items.getOrNull(stackIndex.coerceIn(0,(items.size-1).coerceAtLeast(0)))
            paint.textAlign=Paint.Align.LEFT;paint.typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL)
            paint.textSize=dp(12f);paint.color=android.graphics.Color.WHITE
            canvas.drawText(selected?.title?: "AETHER ISLAND",left+dp(18f),top+dp(19f),paint)
            paint.textSize=dp(10f);paint.typeface=Typeface.create("sans-serif",Typeface.NORMAL);paint.color=0xFFB8C7FF.toInt()
            canvas.drawText(selected?.detail?: "Ready · No active activities",left+dp(18f),top+dp(34f),paint)
            if(state==IslandState.EXPANDED){
                paint.color=0xFF8F9AAF.toInt();paint.textSize=dp(9f)
                canvas.drawText(if(items.isEmpty())"Start a timer or play music" else "Activity ${stackIndex+1}/${items.size} · Swipe left/right to switch",left+dp(18f),top+dp(57f),paint)
                items.take(4).forEachIndexed{index,item->
                    val y=top+dp(78f+index*15f);paint.color=if(index==stackIndex)0xFFB8C7FF.toInt() else 0xFF7F899A.toInt()
                    paint.textSize=dp(9f);canvas.drawText((if(index==stackIndex)"● " else "○ ")+item.title+" · "+item.detail,left+dp(18f),y,paint)
                }
                paint.color=0xFF687386.toInt();paint.textSize=dp(9f)
                canvas.drawText("Tap to collapse · Swipe up to hide",left+dp(18f),top+dp(132f),paint)
            }
        }
        override fun onTouchEvent(event:MotionEvent):Boolean{
            when(event.action){
                MotionEvent.ACTION_DOWN->{downX=event.rawX;downY=event.rawY;moved=false;return true}
                MotionEvent.ACTION_MOVE->{if(abs(event.rawX-downX)>dp(12f)||abs(event.rawY-downY)>dp(12f))moved=true;return true}
                MotionEvent.ACTION_UP->{
                    val dx=event.rawX-downX;val dy=event.rawY-downY
                    if(moved&&dy < -dp(35f))animateTo(IslandState.HIDE)
                    else if(moved&&abs(dx)>dp(35f)&&state==IslandState.EXPANDED){val n=LiveActivityStore.all(this@IslandOverlayService).size;if(n>0)stackIndex=(stackIndex+if(dx<0)1 else -1+n)%n;invalidate()}
                    else if(!moved){when(state){IslandState.HIDE->animateTo(IslandState.COMPACT,420L,true);IslandState.COMPACT->animateTo(IslandState.EXPANDED);IslandState.EXPANDED->animateTo(IslandState.COMPACT)}}
                    performClick();return true
                }
            };return true
        }
        override fun performClick():Boolean{super.performClick();return true}
    }
}
