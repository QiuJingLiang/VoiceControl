package com.scut.vc.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.iflytek.speech.RecognizerResult;
import com.iflytek.speech.SpeechConfig.RATE;
import com.iflytek.speech.SpeechError;
import com.iflytek.ui.RecognizerDialog;
import com.iflytek.ui.RecognizerDialogListener;
import com.scut.vc.identifysemantic.IdentifyThread;
import com.scut.vc.identifysemantic.SemanticIdentify;
import com.scut.vc.utility.Alarm;
import com.scut.vc.utility.AppsManager;
import com.scut.vc.utility.Contact;
import com.scut.vc.utility.DeviceControl;
import com.scut.vc.utility.Task;
import com.scut.vc.utility.WebSearch;
import com.scut.vc.utility.Contact.ContactPerson;
import com.scut.vc.xflib.ChatAdapter;
import com.scut.vc.xflib.ChatEng;

public class MainActivity extends Activity implements RecognizerDialogListener,
		OnClickListener {
	/** Called when the activity is first created. */
	private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;

	private AppsManager mAppManager;
	private Contact mContact;
	private DeviceControl mDevCon;
	private WebSearch mWebSearch;
	private SemanticIdentify mIdentify;

	private ArrayList<ChatEng> list;
	private ChatAdapter cad;
	private ListView chatList;

	private SharedPreferences mSharedPreferences;
	private RecognizerDialog iatDialog;
	private String infos = null;
	public static String voiceString = null;// ���������ṩ�̷��صĴ����ַ���
	public ProgressDialog pd;// ʶ���н�����
	private boolean showProgressDiaglog = false;
	public static boolean EnableGoogleVoice = false;// ʹ��google API
	public static boolean EnableXunfeiVoice = true;// ʹ��Ѷ�� API

	private IdentifyThread mThread;// ����ʶ��Ķ��߳�

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		inital();
		Thread thread = new Thread((mThread = new IdentifyThread(this)));
		thread.start();
		


		/**
		 * ��ȴ���;
		 */
		ArrayList<Contact.ContactPerson> callTarget = new ArrayList<Contact.ContactPerson>();// ��绰�б�
		Contact.ContactPerson contactPerson1 = mContact.new ContactPerson(
				"�й��ƶ�A", "10086");
		
		Contact.ContactPerson contactPerson2 = mContact.new ContactPerson(
				"�й��ƶ�B", "13800138000");
		
		callTarget.add(contactPerson1);
		
		callTarget.add(contactPerson2);
		// Task task = new Task(Task.OpenApp, "com.ihandysoft.alarmclock");
		//Task task = new Task(Task.Search, "com.android.soundrecorder");
		//Task task = new Task(Task.CALL, callTarget);
		//Test(task);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		/*
		 * add()�������ĸ������������ǣ� 1��������������Ļ���дMenu.NONE,
		 * 2��Id���������Ҫ��Android�������Id��ȷ����ͬ�Ĳ˵� 3��˳���Ǹ��˵�������ǰ������������Ĵ�С����
		 * 4���ı����˵�����ʾ�ı�
		 */
		menu.add(Menu.NONE, Menu.FIRST + 1, 1, "����");
		menu.add(Menu.NONE, Menu.FIRST + 2, 1, "����");
		menu.add(Menu.NONE, Menu.FIRST + 3, 3, "�˳�");
		return true;

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		/*
		 * // ��ȡ������ȫ�ֵľ��������Preference SharedPreferences sharedata1 =
		 * getSharedPreferences("list1",MODE_WORLD_READABLE); String data =
		 * sharedata1.getString("item", null); System.out.println("data = " +
		 * data);
		 */

		switch (item.getItemId()) {
		case Menu.FIRST + 1:
			Toast.makeText(this, "�򿪰�������", Toast.LENGTH_SHORT).show();
			Intent intent1 = new Intent();
			intent1.setClass(this, HelpActivity.class);
			startActivity(intent1);
			break;
		case Menu.FIRST + 2:
			Toast.makeText(this, "�����ý���", Toast.LENGTH_SHORT).show();
			Intent intent2 = new Intent();
			intent2.setClass(this, SettingActivity.class);
			startActivity(intent2);
			break;
		case Menu.FIRST + 3:
			Toast.makeText(this, "�˳�Ӧ�ó���", Toast.LENGTH_SHORT).show();
			Intent intent3 = new Intent();
			intent3.setClass(this, HelpActivity.class);
			startActivity(intent3);
			break;
		}
		return false;
	}

	/**
	 * ��ʼ��ʵ����
	 */
	private void inital() {
		/**
		 * ��ʼ��һЩ���ƶ���
		 */
		mAppManager = new AppsManager(this);
		mContact = new Contact(this);
		mDevCon = new DeviceControl(this);
		mWebSearch = new WebSearch(this);
		mIdentify = new SemanticIdentify(this);

		list = new ArrayList<ChatEng>();
		cad = new ChatAdapter(MainActivity.this, list);
		chatList = (ListView) findViewById(R.id.chatlist);
		ImageButton ib = (ImageButton) findViewById(R.id.helper_voice);

		/**
		 * �������ʱ��progressBar��ʾ
		 */
		pd = new ProgressDialog(this);
		pd.setMessage("���ڽ���...");

		/**
		 * Ѷ�ɴ��ڳ�ʼ��
		 */
		iatDialog = new RecognizerDialog(this, "appid="
				+ getString(R.string.app_id));
		iatDialog.setListener(this);

		/**
		 * �Ի�Ͳ��ť����Ӧ
		 */
		ib.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				// TODO Auto-generated method stub

				// ��ȡ��������ѡ�������
				// ��һ�������汾�ϵļ���
				// SharedPreferences sharedata1 = getSharedPreferences(
				// "voiceEngine", MODE_WORLD_READABLE | MODE_MULTI_PROCESS);
				SharedPreferences sharedata1 = getSharedPreferences(
						"voiceEngine", MODE_WORLD_READABLE);
				String voiceEngine = sharedata1.getString("voiceEngine", "1");// ���������ȷ��ȡ��������ѡ������ݣ����Ե�һ��Ϊֵ
				System.out.println("voiceEngine = " + voiceEngine);

				if (voiceEngine.equals("1")) {// EnableGoogleVoice
					startVoiceRecognitionActivity();
				} else if (voiceEngine.equals("2")) {// EnableXunfeiVoice
					showIatDialog();
				}
			}

		});

	}

	/**
	 * ����Ĵ���
	 */
	public Handler mhandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub

			Task task = (Task) msg.obj;
			switch (task.getTaskID()) {
			case Task.CALL: {
				@SuppressWarnings("unchecked")
				ArrayList<Contact.ContactPerson> callList = (ArrayList<Contact.ContactPerson>) task
						.getTaskParam();
				if (0 == callList.size()) {
					mAppManager.Execute("com.android.contacts");
				} else if (1 == callList.size()) {
					String phoneNum = callList.get(0).GetNumber();
					mContact.CallPerson(phoneNum);
				} else if (1 < callList.size()) {
					ShowSelectDialog(callList, task);
				}
			}
				break;
			case Task.SendMessage: {
				String number = (String) task.getTaskParam();
				mContact.SendMsg(number, "ooo");
			}
				break;
			case Task.OpenApp: {
				String packname = (String) task.getTaskParam();
				mAppManager.Execute(packname);
			}
				break;
			case Task.Search: {
				String search = (String) task.getTaskParam();
				mWebSearch.Execute(search);
			}
				break;
			case Task.SwitchOnDevice: {
				String device = (String) task.getTaskParam();
				mDevCon.EnableDevice(device);
			}
				break;
			case Task.SetAlarm: {
				String strvoice = (String) task.getTaskParam();
				Alarm alarm = new Alarm(MainActivity.this, strvoice);
				alarm.Execute();
			}
				break;
			case Task.ShowProcess: {
				if (!showProgressDiaglog) {
					pd.show();
					showProgressDiaglog = true;
				} else {
					pd.cancel();
					showProgressDiaglog = false;
				}
			}
				break;
			case Task.IdentifyError: {
				updateListView(R.layout.chat_helper, "�Բ���Ŷ���Ҳ����������");
			}
			default: {
				// updateListView("�Բ���Ŷ���Ҳ������");
			}
			}

			super.handleMessage(msg);
		}
	};

	/**
	 * ����ǻ���������resId�͸�ֵR.layout.chat_helper; ������˽�����resId�͸�ֵR.layout.chat_user
	 * 
	 * @param resId
	 * @param speekInfo
	 */
	public void updateListView(int resId, String speekInfo) {

		ChatEng ce = new ChatEng(speekInfo, resId);
		list.add(ce);
		chatList.setAdapter(cad);
		// cad.notify();

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	/**
	 * Fire an intent to start the speech recognition activity.
	 */
	private void startVoiceRecognitionActivity() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
				RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
				"Speech recognition demo");
		startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
	}

	/**
	 * Handle the results from the recognition activity. �ȸ�API���صĽ��
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == VOICE_RECOGNITION_REQUEST_CODE
				&& resultCode == RESULT_OK) {
			// Fill the list view with the strings the recognizer thought it
			// could have heard
			ArrayList matches = data
					.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

			voiceString = matches.get(0).toString();
			updateListView(R.layout.chat_user, voiceString);
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	public void showIatDialog() {
		// TODO Auto-generated method stub
		String engine = "sms";
		String area = null;

		iatDialog.setEngine(engine, area, null);
		iatDialog.setSampleRate(RATE.rate8k);
		infos = null;
		iatDialog.show();

	}

	@Override
	public void onStart() {
		super.onStart();

		String engine = "sms";
		String[] engineEntries = getResources().getStringArray(
				R.array.preference_entries_iat_engine);
		String[] engineValues = getResources().getStringArray(
				R.array.preference_values_iat_engine);
		for (int i = 0; i < engineValues.length; i++) {
			if (engineValues[i].equals(engine)) {
				infos = engineEntries[i];
				break;
			}
		}

	}

	public void onClick(View v) {
		// TODO Auto-generated method stub

	}

	public void onEnd(SpeechError arg0) {
		// TODO Auto-generated method stub

	}

	/**
	 * Ѷ�ɴ��صĽ��
	 */
	public void onResults(ArrayList<RecognizerResult> arg0, boolean arg1) {
		// TODO Auto-generated method stub
		voiceString = "";
		for (int i = 0; i < arg0.size(); i++) {
			RecognizerResult recognizerResult = arg0.get(i);
			voiceString += recognizerResult.text;
		}
		if (voiceString.equals("��") == false) {
			updateListView(R.layout.chat_user, voiceString);
		}

	}

	/**
	 * ���б��������
	 * 
	 * @param items
	 * @param task
	 */
	public void ShowSelectDialog(final ArrayList<Contact.ContactPerson> list,
			final Task task) {
		final String[] items = new String[list.size()];
		for (int n = 0; n < list.size(); n++) {
			items[n] = ((Contact.ContactPerson) list.get(n)).GetName();
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("��ѡ��").setItems(items,
				new android.content.DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						ArrayList<Contact.ContactPerson> _list = new ArrayList<Contact.ContactPerson> ();
						_list.add(list.get(which));
						Task _task = new Task(task.getTaskID(), _list);
						Message msg = new Message();
						msg.obj = _task;
						mhandler.sendMessage(msg);
					}
				});

		AlertDialog dialog = builder.create();
		dialog.show();

	}

	/**
	 * ����ڴ�
	 */
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		mAppManager = null;
		mContact = null;
		mDevCon.Release();
		mDevCon = null;
		mWebSearch = null;
		mIdentify = null;
		list = null;
		cad = null;
		chatList = null;
		mSharedPreferences = null;
		iatDialog = null;
		voiceString = null;// ���������ṩ�̷��صĴ����ַ���
		pd = null;
		mThread = null;// ����ʶ��Ķ��߳�
		super.onDestroy();
	}

	private void Test(Task task) {
		Message msg = new Message();
		msg.obj = task;
		mhandler.sendMessage(msg);
	}
}