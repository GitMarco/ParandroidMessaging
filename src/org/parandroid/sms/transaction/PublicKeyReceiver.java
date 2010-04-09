package org.parandroid.sms.transaction;

import java.util.ArrayList;

import org.parandroid.encryption.MessageEncryptionFactory;
import org.parandroid.sms.R;
import org.parandroid.sms.ui.MessageUtils;
import org.parandroid.sms.ui.MessagingPreferenceActivity;
import org.parandroid.sms.ui.PublicKeyReceived;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Telephony.Sms.Intents;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;

public class PublicKeyReceiver extends BroadcastReceiver {

	private static final String TAG = "PublicKeyReceiver";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String uricontent = intent.getDataString();
        String port = uricontent.substring(uricontent.lastIndexOf(":") + 1);
        Log.v(TAG, "Reveiced package on port " + port);
        if(Integer.parseInt(port) != MessageEncryptionFactory.PUBLIC_KEY_PORT) return;
        
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		String message = context.getString(R.string.received_public_key);
		
		SmsMessage[] messages = Intents.getMessagesFromIntent(intent);
		if(messages.length == 0) return;
		
		SmsMessage msg = messages[0];

		Intent targetIntent = new Intent(context, PublicKeyReceived.class);
		targetIntent.putExtra("sender", msg.getDisplayOriginatingAddress());
		
		int len = 0, offset = 0;
		ArrayList<byte[]> publicKey = new ArrayList<byte[]>();
		for(SmsMessage m : messages){
			byte[] data = m.getUserData();
			len += data.length;
			publicKey.add(data);
		}
		byte[] pubKeyData = new byte[len];
		for(byte[] part : publicKey){
			System.arraycopy(part, 0, pubKeyData, offset, part.length);
			offset += part.length;
		}

		int notificationId = MessageUtils.getNotificationId(msg.getOriginatingAddress()); 
		
		targetIntent.putExtra("publickey", pubKeyData);
		targetIntent.putExtra("notificationId", notificationId);
		Notification n = new Notification(context, R.drawable.stat_notify_public_key_recieved, message, System.currentTimeMillis(), message, message, targetIntent);
		
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		if (sp.getBoolean(MessagingPreferenceActivity.NOTIFICATION_ENABLED, true)) {
			if(sp.getBoolean(MessagingPreferenceActivity.NOTIFICATION_VIBRATE, true))
				n.defaults |= Notification.DEFAULT_VIBRATE;
			
			String ringtoneStr = sp.getString(MessagingPreferenceActivity.NOTIFICATION_RINGTONE, null);
			n.sound = TextUtils.isEmpty(ringtoneStr) ? null : Uri.parse(ringtoneStr);
			
			n.flags |= Notification.FLAG_SHOW_LIGHTS;
	        n.ledARGB = 0xff00ff00;
	        n.ledOnMS = 500;
	        n.ledOffMS = 2000;
        }
		
		mNotificationManager.notify(notificationId, n);
	}
		
}