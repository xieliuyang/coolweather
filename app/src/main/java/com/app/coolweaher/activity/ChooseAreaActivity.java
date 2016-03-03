package com.app.coolweaher.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.app.coolweaher.R;
import com.app.coolweaher.db.CoolWeatherDB;
import com.app.coolweaher.model.City;
import com.app.coolweaher.model.County;
import com.app.coolweaher.model.Province;
import com.app.coolweaher.util.HttpCallbackListener;
import com.app.coolweaher.util.HttpUtil;
import com.app.coolweaher.util.Utility;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liuyang on 2016/3/2.
 */
public class ChooseAreaActivity extends Activity {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;
    private TextView titleText;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private CoolWeatherDB coolWeatherDB;
    private List<String> datalist=new ArrayList<String>();
    /**
     * 省列表
     */
    private List<Province> provinceList;
    /**
     * 市列表
     */
    private List<City> cityList;
    /**
     * 县列表
     */
    private List<County> countyList;
    /**
     * 选中的省份
     */
    private Province selectProvince;
    /**
     * 选中的市
     */
    private City selectCity;
    /**
     * 选中的县
     */
    private County selectCounty;

    /**
     *当前选中的级别
     */
    private int currentLevel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.choose_area);
        listView=(ListView)findViewById(R.id.list_view);
        titleText = (TextView)findViewById(R.id.title_text);
        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_expandable_list_item_1,datalist);
        listView.setAdapter(adapter);
        coolWeatherDB = CoolWeatherDB.getInstance(this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int index, long arg3) {
                if (currentLevel == LEVEL_PROVINCE){
                   selectProvince= provinceList.get(index);
                    queryCities();
                }else if (currentLevel == LEVEL_CITY){
                    selectCity = cityList.get(index);
                    queryCounties();

                }
            }
        });
        queryProvinces();

    }


    /**
     * 查询所有省份，优先从数据库获得，没有查询再去服务器
     */
    private void queryProvinces(){
        provinceList = coolWeatherDB.loadProvinces();
        if (provinceList.size()>0){
            datalist.clear();
            for (Province province : provinceList){
                datalist.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText("中国");
            currentLevel = LEVEL_PROVINCE;
        }else{
            queryFromServer(null,"province");
        }
    }
    /**
     * 查询所选省份所有市，优先从数据库获得，没有查询再去服务器
     */
    private void queryCities(){
        cityList = coolWeatherDB.loadCities(selectProvince.getId());
        if (cityList.size()>0){
            datalist.clear();
            for (City city : cityList){
                datalist.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectProvince.getProvinceName());
            currentLevel = LEVEL_CITY;
        }else{
            queryFromServer(selectProvince.getProvinceCode(),"city");
        }
    }

    /**
     * 查询所选市所有县，优先从数据库获得，没有查询再去服务器
     */
    private void queryCounties(){
        countyList = coolWeatherDB.loadCounties(selectCity.getId());
        if (countyList.size()>0){
            datalist.clear();
            for (County county : countyList){
                datalist.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectCity.getCityName());
            currentLevel = LEVEL_COUNTY;
        }else{
            queryFromServer(selectCity.getCityCode(),"city");
        }
    }

    /**
     * 根据传入的代号和类型从服务器上查询省市县数据
     */
    private  void queryFromServer (final String code,final  String type){
        String address;
        if (!TextUtils.isEmpty(code)){
            address = "http://www.weather.com.cn/data/list3/city"+code+".xml";
        }else{
            address= "http://www.weather.com.cn/data/list3/city.xml";
        }
        showProgressDialog();
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                boolean result = false;
                if ("province".equals(type)) {
                    result = Utility.handleProvincesResponse(coolWeatherDB, response);
                } else if ("city".equals(type)) {
                    result = Utility.handleCitiesResponse(coolWeatherDB, response, selectProvince.getId());
                } else if ("county".equals(type)) {
                    result = Utility.handleCountiesResponse(coolWeatherDB, response, selectCity.getId());
                }
                if (result) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)) {
                                queryProvinces();
                            } else if ("city".equals(type)) {
                                queryCities();
                            } else if ("county".equals(type)) {
                                queryCounties();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(ChooseAreaActivity.this, "加载失败", Toast.LENGTH_LONG).show();
                    }
                });

            }
        });
    }
    /**
     * 显示进度对话框
     */
    private void showProgressDialog(){
        if (progressDialog == null){
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("正在加载。。。");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }
    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog(){
        if (progressDialog != null){
            progressDialog.dismiss();
        }
    }
    /**
     * 捕获back按键，根据当前级别判断返回上级菜单还是退出
     */
    @Override
    public void onBackPressed() {
        if (currentLevel== LEVEL_COUNTY){
            queryCities();
        }else if (currentLevel==LEVEL_CITY){
            queryProvinces();
        }else {
            finish();
        }
    }
}
