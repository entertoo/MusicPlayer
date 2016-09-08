package com.example.musicplayer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author haopi
 * @创建时间 2016年7月7日 下午2:47:35
 * @描述 splash数据操作工具
 * 
 * @ 修改提交者:$Author: chp $ @ 提交时间:$Date: 2016-07-14 20:42:00 +0800 (Thu, 14 Jul
 * 2016) $ @ 当前版本 $Rev: 19 $
 * 
 */
public class SpTools
{
	public class MyConstants{

		public static final String CONFIGFILE = "config";
	}

	public static void setInt(Context context, String key, int value) {
		SharedPreferences sp = context.getSharedPreferences(MyConstants.CONFIGFILE, Context.MODE_PRIVATE);
		Editor edit = sp.edit();
		edit.putInt(key, value);
		edit.commit();
	}
	
	public static int getInt(Context context, String key, int defValue){
		SharedPreferences sp = context.getSharedPreferences(MyConstants.CONFIGFILE, Context.MODE_PRIVATE);
		int value = sp.getInt(key, defValue);
		return value;
	}

	/**
	 * 如果vlaue为true，表示已经安装设置过数据，反之没有
	 * 
	 * @param context
	 * @param key
	 * @param value
	 */
	public static void setBoolean(Context context, String key, boolean value) {
		SharedPreferences sp = context.getSharedPreferences(MyConstants.CONFIGFILE, Context.MODE_PRIVATE);
		Editor edit = sp.edit();
		edit.putBoolean(key, value);
		edit.commit();
	}

	/**
	 * 获取用户是否安装设置过数据，默认为false表示未设置过
	 * 
	 * @param context
	 * @param key
	 * @param defValue
	 * @return
	 */
	public static boolean getBoolean(Context context, String key, boolean defValue) {
		SharedPreferences sp = context.getSharedPreferences(MyConstants.CONFIGFILE, Context.MODE_PRIVATE);
		boolean value = sp.getBoolean(key, defValue);
		return value;
	}

	/**
	 * 保存缓存网络数据
	 * 
	 * @param context
	 * @param key
	 * @param value
	 */
	public static void setString(Context context, String key, String value) {
		SharedPreferences sp = context.getSharedPreferences(MyConstants.CONFIGFILE, Context.MODE_PRIVATE);
		Editor edit = sp.edit();
		edit.putString(key, value);
		edit.commit();
	}

	/**
	 * 获取缓存网络数据
	 * 
	 * @param context
	 * @param key
	 * @param defValue
	 * @return
	 */
	public static String getString(Context context, String key, String defValue) {
		SharedPreferences sp = context.getSharedPreferences(MyConstants.CONFIGFILE, Context.MODE_PRIVATE);
		String value = sp.getString(key, defValue);
		return value;
	}

	public static void setSet(Context context, String key, Set<String> values) {
		SharedPreferences sp = context.getSharedPreferences(MyConstants.CONFIGFILE, Context.MODE_PRIVATE);
		Editor edit = sp.edit();
		edit.putStringSet(key, values);
		edit.commit();
	}

	/**
	 * 获取缓存网络数据
	 * 
	 * @param context
	 * @param key
	 * @param defValue
	 * @return
	 */
	public static Set<String> getSet(Context context, String key, Set<String> defValue) {
		SharedPreferences sp = context.getSharedPreferences(MyConstants.CONFIGFILE, Context.MODE_PRIVATE);
		Set<String> value = sp.getStringSet(key, defValue);
		return value;
	}

	/** 数据存储的XML名称 **/
	public final static String SETTING = "SharedPrefsStrList";

	/**
	 * 存储数据(Int)
	 * 
	 * @param context
	 * @param key
	 * @param value
	 */
	private static void putIntValue(Context context, String key, int value) {
		Editor sp = context.getSharedPreferences(SETTING, Context.MODE_PRIVATE).edit();
		sp.putInt(key, value);
		sp.commit();
	}

	/**
	 * 存储数据(String)
	 * 
	 * @param context
	 * @param key
	 * @param value
	 */
	private static void putStringValue(Context context, String key, String value) {
		Editor sp = context.getSharedPreferences(SETTING, Context.MODE_PRIVATE).edit();
		sp.putString(key, value);
		sp.commit();
	}

	/**
	 * 存储List<String>
	 * 
	 * @param context
	 * @param key
	 *            List<String>对应的key
	 * @param strList
	 *            对应需要存储的List<String>
	 */
	public static void putStrListValue(Context context, String key, List<String> strList) {
		if (null == strList) {
			return;
		}
		// 保存之前先清理已经存在的数据，保证数据的唯一性
		removeStrList(context, key);
		int size = strList.size();
		putIntValue(context, key + "size", size);
		for (int i = 0; i < size; i++) {
			putStringValue(context, key + i, strList.get(i));
		}
	}

	/**
	 * 取出数据（int)
	 * 
	 * @param context
	 * @param key
	 * @param defValue
	 *            默认值
	 * @return
	 */
	private static int getIntValue(Context context, String key, int defValue) {
		SharedPreferences sp = context.getSharedPreferences(SETTING, Context.MODE_PRIVATE);
		int value = sp.getInt(key, defValue);
		return value;
	}

	/**
	 * 取出数据（String)
	 * 
	 * @param context
	 * @param key
	 * @param defValue
	 *            默认值
	 * @return
	 */
	private static String getStringValue(Context context, String key, String defValue) {
		SharedPreferences sp = context.getSharedPreferences(SETTING, Context.MODE_PRIVATE);
		String value = sp.getString(key, defValue);
		return value;
	}

	/**
	 * 取出List<String>
	 * 
	 * @param context
	 * @param key
	 *            List<String> 对应的key
	 * @return List<String>
	 */
	public static List<String> getStrListValue(Context context, String key) {
		List<String> strList = new ArrayList<String>();
		int size = getIntValue(context, key + "size", 0);
		// Log.d("sp", "" + size);
		for (int i = 0; i < size; i++) {
			strList.add(getStringValue(context, key + i, null));
		}
		return strList;
	}

	/**
	 * 清空List<String>所有数据
	 * 
	 * @param context
	 * @param key
	 *            List<String>对应的key
	 */
	public static void removeStrList(Context context, String key) {
		int size = getIntValue(context, key + "size", 0);
		if (0 == size) {
			return;
		}
		remove(context, key + "size");
		for (int i = 0; i < size; i++) {
			remove(context, key + i);
		}
	}

	/**
	 * @Description TODO 清空List<String>单条数据
	 * @param context
	 * @param key
	 *            List<String>对应的key
	 * @param str
	 *            List<String>中的元素String
	 */
	public static void removeStrListItem(Context context, String key, String str) {
		int size = getIntValue(context, key + "size", 0);
		if (0 == size) {
			return;
		}
		List<String> strList = getStrListValue(context, key);
		// 待删除的List<String>数据暂存
		List<String> removeList = new ArrayList<String>();
		for (int i = 0; i < size; i++) {
			if (str.equals(strList.get(i))) {
				if (i >= 0 && i < size) {
					removeList.add(strList.get(i));
					remove(context, key + i);
					putIntValue(context, key + "size", size - 1);
				}
			}
		}
		strList.removeAll(removeList);
		// 删除元素重新建立索引写入数据
		putStrListValue(context, key, strList);
	}

	/**
	 * 清空对应key数据
	 * 
	 * @param context
	 * @param key
	 */
	public static void remove(Context context, String key) {
		Editor sp = context.getSharedPreferences(SETTING, Context.MODE_PRIVATE).edit();
		sp.remove(key);
		sp.commit();
	}

	/**
	 * 清空所有数据
	 * 
	 * @param context
	 */
	public static void clear(Context context) {
		Editor sp = context.getSharedPreferences(SETTING, Context.MODE_PRIVATE).edit();
		sp.clear();
		sp.commit();
	}
}
