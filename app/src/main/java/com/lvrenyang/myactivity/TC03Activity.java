package com.lvrenyang.myactivity;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
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

public class TC03Activity extends Activity implements OnClickListener {

	private EditText editTextSend;
	private TextView textViewRecv;
	private CheckBox checkBoxSendToUart;

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
		setContentView(R.layout.activity_tc03);

		findViewById(R.id.buttonClearRecv).setOnClickListener(this);
		findViewById(R.id.buttonClearSend).setOnClickListener(this);
		findViewById(R.id.buttonSendCmd).setOnClickListener(this);
		findViewById(R.id.buttonSendText).setOnClickListener(this);
		editTextSend = (EditText) findViewById(R.id.editTextSend);
		textViewRecv = (TextView) findViewById(R.id.textViewRecv);
		checkBoxSendToUart = (CheckBox) findViewById(R.id.checkBoxSendToUart);

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
		case R.id.buttonSendCmd:
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
				if (checkBoxSendToUart.isChecked())
					WorkService.workThread.handleCmd(
							Global.CMD_EMBEDDED_SEND_TO_UART, data);
				else
					WorkService.workThread.handleCmd(Global.CMD_WRITE, data);
			} else {
				Toast.makeText(this, Global.toast_notconnect,
						Toast.LENGTH_SHORT).show();
			}
			break;

		case R.id.buttonSendText: {
			if (WorkService.workThread.isConnected()) {

				String text = editTextSend.getText().toString()
						+ "\r\n\r\n\r\n"; // 加三行换行，避免走纸
				byte header[] = null;
				byte strbuf[] = null;

				// 设置UTF8编码
				// Android手机默认也是UTF8编码
				header = new byte[] { 0x1b, 0x40, 0x1c, 0x26, 0x1b, 0x39, 0x01 };
				try {
					strbuf = text.getBytes("UTF-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				byte buffer[] = DataUtils.byteArraysToBytes(new byte[][] {
						header, strbuf });
				Bundle data = new Bundle();
				data.putByteArray(Global.BYTESPARA1, buffer);
				data.putInt(Global.INTPARA1, 0);
				data.putInt(Global.INTPARA2, buffer.length);
				if (checkBoxSendToUart.isChecked())
					WorkService.workThread.handleCmd(
							Global.CMD_EMBEDDED_SEND_TO_UART, data);
				else
					WorkService.workThread.handleCmd(Global.CMD_WRITE, data);

			} else {
				Toast.makeText(this, Global.toast_notconnect,
						Toast.LENGTH_SHORT).show();
			}
			break;
		}

		case R.id.buttonClearRecv:
			textViewRecv.setText("");
			break;

		case R.id.buttonClearSend:
			editTextSend.setText("");
			break;
		}
	}

	static class MHandler extends Handler {

		WeakReference<TC03Activity> mActivity;

		MHandler(TC03Activity activity) {
			mActivity = new WeakReference<TC03Activity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			TC03Activity theActivity = mActivity.get();
			switch (msg.what) {

			case Global.CMD_WRITERESULT:
			case Global.CMD_EMBEDDED_SEND_TO_UART_RESULT: {
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