package vn.aether.island

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService

class AetherMediaListener : NotificationListenerService() {
    private val callbacks = mutableMapOf<MediaController, MediaController.Callback>()
    override fun onListenerConnected() { super.onListenerConnected(); refreshSessions() }
    override fun onListenerDisconnected() {
        callbacks.forEach { (controller, callback) -> try { controller.unregisterCallback(callback) } catch (_:Exception) {} }
        callbacks.clear()
        super.onListenerDisconnected()
    }
    override fun onNotificationPosted(sbn: android.service.notification.StatusBarNotification?) { refreshSessions() }
    override fun onNotificationRemoved(sbn: android.service.notification.StatusBarNotification?) { refreshSessions() }
    private fun refreshSessions() {
        val sessions = try { getSystemService(android.media.session.MediaSessionManager::class.java).getActiveSessions(ComponentName(this,AetherMediaListener::class.java)) } catch (_:Exception) { emptyList<MediaController>() }
        val active = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: sessions.firstOrNull { it.metadata != null }
        if (active == null) { LiveActivityStore.remove(this,"music"); return }
        sessions.forEach { controller ->
            if (!callbacks.containsKey(controller)) {
                val callback=object:MediaController.Callback() {
                    override fun onMetadataChanged(metadata:MediaMetadata?) { publish(controller) }
                    override fun onPlaybackStateChanged(state:PlaybackState?) { refreshSessions() }
                }
                callbacks[controller]=callback
                try { controller.registerCallback(callback) } catch (_:Exception) {}
            }
        }
        publish(active)
    }
    private fun publish(controller:MediaController) {
        val metadata=controller.metadata
        val title=metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: controller.packageName
        val artist=metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Now playing"
        val playing=controller.playbackState?.state == PlaybackState.STATE_PLAYING
        LiveActivityStore.put(this,LiveItem("music",title,artist+" · "+if(playing)"Playing" else "Paused","music",System.currentTimeMillis()))
    }
}
