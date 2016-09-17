package com.lvrenyang.myactivity;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.lvrenyang.callback.RecvCallBack;
import com.lvrenyang.myprinter.WorkService;
import com.lvrenyang.myprinter.Global;
import com.lvrenyang.myprinter.R;
import com.lvrenyang.rwbt.BTRWThread;
import com.lvrenyang.rwusb.USBRWThread;
import com.lvrenyang.rwwifi.NETRWThread;
import com.lvrenyang.utils.DataUtils;

public class CmdActivity extends Activity implements OnClickListener {

	private EditText editTextSend;
	private TextView textViewRecv;

	private static Handler mHandler = null;
	private static String TAG = "CmdActivity";

	RecvCallBack callback = new RecvCallBack() {

		public void onRecv(byte[] buffer, int byteOffset, int byteCount) {
			// TODO Auto-generated method stub
			if (null != mHandler) {
				Message msg = mHandler.obtainMessage(Global.MSG_ON_RECIVE);
				Bundle data = new Bundle();
				byte[] buf = new byte[byteCount];
				DataUtils.copyBytes(buffer, byteOffset, buf, 0, byteCount);
				data.putByteArray(Global.BYTESPARA1, buf);
				data.putInt(Global.INTPARA1, 0);
				data.putInt(Global.INTPARA2, buf.length);
				msg.setData(data);
				mHandler.sendMessage(msg);
			}
			String str = DataUtils.BytesToHexStr(buffer, byteOffset, byteCount)
					.toString() + "\r\n";
			Log.i(TAG, str);
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cmd);

		findViewById(R.id.buttonClearRecv).setOnClickListener(this);
		findViewById(R.id.buttonClearSend).setOnClickListener(this);
		findViewById(R.id.buttonSend).setOnClickListener(this);
		editTextSend = (EditText) findViewById(R.id.editTextSend);
		textViewRecv = (TextView) findViewById(R.id.textViewRecv);

		mHandler = new MHandler(this);

		WorkService.workThread.handleCmd(Global.MSG_PAUSE_HEARTBEAT, null);
		BTRWThread.SetOnRecvCallBack(callback);
		NETRWThread.SetOnRecvCallBack(callback);
		USBRWThread.SetOnRecvCallBack(callback);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		USBRWThread.SetOnRecvCallBack(null);
		NETRWThread.SetOnRecvCallBack(null);
		BTRWThread.SetOnRecvCallBack(null);
		WorkService.workThread.handleCmd(Global.MSG_RESUME_HEARTBEAT, null);
	}

	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		switch (arg0.getId()) {
		case R.id.buttonSend:
			String str = editTextSend.getText().toString().trim();
			str = DataUtils.RemoveChar(str, ' ').toString();
			if (str.length() <= 0)
				break;
			if ((str.length() % 2) != 0) {
				Toast.makeText(this, "Format Error", Toast.LENGTH_SHORT).show();
				break;
			}

			byte[] buf = DataUtils.HexStringToBytes(str);

			if (WorkService.workThread.isConnected()) {
				Bundle data = new Bundle();
				data.putByteArray(Global.BYTESPARA1, buf);
				data.putInt(Global.INTPARA1, 0);
				data.putInt(Global.INTPARA2, buf.length);
				WorkService.workThread.handleCmd(Global.CMD_WRITE, data);
			} else {
				Toast.makeText(this, Global.toast_notconnect,
						Toast.LENGTH_SHORT).show();
			}
			break;

		case R.id.buttonClearRecv:
			textViewRecv.setText("");
			break;

		case R.id.buttonClearSend:
			editTextSend.setText("");
			break;
		}
	}

	static class MHandler extends Handler {

		WeakReference<CmdActivity> mActivity;

		MHandler(CmdActivity activity) {
			mActivity = new WeakReference<CmdActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			CmdActivity theActivity = mActivity.get();
			switch (msg.what) {

			case Global.CMD_WRITERESULT: {
				int result = msg.arg1;
				Toast.makeText(
						theActivity,
						(result == 1) ? Global.toast_success
								: Global.toast_fail, Toast.LENGTH_SHORT).show();
				Log.v(TAG, "Result: " + result);
				break;
			}

			case Global.MSG_ON_RECIVE: {
				Log.v(TAG, "OnRecv");
				// 将接收到的数据显示出来。
				Bundle data = msg.getData();
				byte[] buffer = data.getByteArray(Global.BYTESPARA1);
				int offset = data.getInt(Global.INTPARA1);
				int count = data.getInt(Global.INTPARA2);
				String str = DataUtils.BytesToHexStr(buffer, offset, count)
						.toString() + "\r\n";
				theActivity.textViewRecv.append(str);
				break;
			}

			}
		}
	}
}