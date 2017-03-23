package android.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;


import com.mods.grx.settings.GrxPreferenceScreen;
import com.mods.grx.settings.R;

import com.mods.grx.settings.dlgs.DlgFrSelectSortItems;
import com.mods.grx.settings.Common;
import com.mods.grx.settings.prefssupport.info.PrefAttrsInfo;
import com.mods.grx.settings.utils.Utils;

import java.util.regex.Pattern;

/*      2017 Grouxho (esp-desarrolladores.com)

        Licensed under the Apache License, Version 2.0 (the "License");
        you may not use this file except in compliance with the License.
        You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.
*/


public class GrxSelectSortItems extends Preference implements DlgFrSelectSortItems.GrxMultiValueListener,
        GrxPreferenceScreen.CustomDependencyListener{


    private Runnable RDobleClick;
    private Handler handler;
    private boolean doble_clic_pendiente;
    private Long timeout;
    private int clicks;

    private String mLabel;

    private PrefAttrsInfo myPrefAttrsInfo;
    private String mValue="";

    private int id_options_array;
    private int id_values_array;
    private int id_icons_array;
    private ImageView vWidgetArrow;
    private ImageView vAndroidIcon;

    public GrxSelectSortItems(Context c){
        super(c);
    }

    public GrxSelectSortItems(Context c, AttributeSet a){
        super(c,a);
        ini_param(c,a);
    }


    public GrxSelectSortItems(Context context, AttributeSet attrs, int defStyleAttr){
        super(context,attrs,defStyleAttr);
        ini_param(context,attrs);
    }

    private void ini_param(Context context, AttributeSet attrs){

        clicks=0;
        myPrefAttrsInfo = new PrefAttrsInfo(context, attrs, getTitle(), getSummary(),getKey(), true);
        id_options_array = Utils.get_array_id(getContext(),attrs.getAttributeValue(null,"grxA_entries"));
        id_values_array = Utils.get_array_id(getContext(),attrs.getAttributeValue(null,"grxA_values"));
        id_icons_array = Utils.get_array_id(getContext(),attrs.getAttributeValue(null,"grxA_ics"));
        setDefaultValue(myPrefAttrsInfo.get_my_string_def_value());
    }





    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            mValue=getPersistedString(myPrefAttrsInfo.get_my_string_def_value());
        } else {
            mValue= myPrefAttrsInfo.get_my_string_def_value();
            if(!myPrefAttrsInfo.is_valid_key()) return;
            persistString(mValue);
        }
        save_value_in_settings_db(mValue);
        config_preference(mValue);
        setSummary(mLabel);
    }

    private void save_value_in_settings_db(String value){
        if(!myPrefAttrsInfo.is_valid_key()) return;
        if(myPrefAttrsInfo.get_allowed_save_in_settings_db()) {
            String real = Settings.System.getString(getContext().getContentResolver(), this.getKey());
            if(real==null) real="N/A";
            if (!real.equals(value)) {
                Settings.System.putString(getContext().getContentResolver(), this.getKey(), value);
            }
        }
    }



    private void set_up_double_click(){
        handler = new Handler();
        timeout = Long.valueOf(ViewConfiguration.getDoubleTapTimeout());
        doble_clic_pendiente=false;

        RDobleClick = new Runnable() {
            @Override
            public void run() {
                if(!doble_clic_pendiente){
                    accion_click();
                }else {
                    if(clicks==0) accion_click();
                    else if(clicks!=2) {
                        accion_click();
                    }else {
                        accion_doble_click();
                    }

                }
            }
        };
    }

    @Override
    protected void onClick() {
        if(handler==null) set_up_double_click();
        if(RDobleClick==null) show_dialog();
        else{
            clicks++;
            if(!doble_clic_pendiente){
                handler.removeCallbacks(RDobleClick);
                doble_clic_pendiente=true;
                handler.postDelayed(RDobleClick,timeout);
            }
        }
    }

    private void accion_click(){
        clicks=0;
        doble_clic_pendiente=false;
        handler.removeCallbacks(RDobleClick);
        show_dialog();
    }




    private void show_dialog(){

        GrxPreferenceScreen prefsScreen = (GrxPreferenceScreen) getOnPreferenceChangeListener();
        if(prefsScreen!=null){
            DlgFrSelectSortItems dlg =(DlgFrSelectSortItems) prefsScreen.getFragmentManager().findFragmentByTag(Common.TAG_DLGFRGRMULTIVALUES);
            if(dlg==null){
                dlg = DlgFrSelectSortItems.newInstance(this, Common.TAG_PREFSSCREEN_FRAGMENT, myPrefAttrsInfo.get_my_key(), myPrefAttrsInfo.get_my_title(),mValue,
                        id_options_array, id_values_array, id_icons_array, myPrefAttrsInfo.get_my_separator(), myPrefAttrsInfo.get_my_max_items());
                dlg.show(prefsScreen.getFragmentManager(),Common.TAG_DLGFRGRMULTIVALUES);
            }
        }

    }


    private void accion_doble_click(){
        clicks=0;
        doble_clic_pendiente=false;
        handler.removeCallbacks(RDobleClick);
        reset_value();
    }


    private void save_value(String value) {
        mValue=value;
        if(!myPrefAttrsInfo.is_valid_key()) return;
        persistString(mValue);
        notifyChanged();
        getOnPreferenceChangeListener().onPreferenceChange(this,mValue);
        save_value_in_settings_db(mValue);
        send_broadcasts_and_change_group_key();
    }


    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        vWidgetArrow = (ImageView) view.findViewById(R.id.widget_arrow);
        vAndroidIcon = (ImageView) view.findViewById(android.R.id.icon);
        vAndroidIcon.setLayoutParams(Common.AndroidIconParams);
        return view;
    }

    @Override
    public void onBindView(View view) {
        float alpha = (isEnabled() ? (float) 1.0 : (float) 0.4);
        if (vWidgetArrow != null) vWidgetArrow.setAlpha(alpha);
        if (vAndroidIcon != null) vAndroidIcon.setAlpha(alpha);
        setSummary(mLabel);
        super.onBindView(view);

    }

    private void reset_value(){
        if(mValue.isEmpty()) return;
        AlertDialog dlg = new AlertDialog.Builder(getContext()).create();
        dlg.setTitle(getContext().getResources().getString(R.string.gs_tit_reset));
        dlg.setMessage(getContext().getResources().getString(R.string.gs_mensaje_reset));
        dlg.setButton(DialogInterface.BUTTON_POSITIVE, getContext().getString(R.string.gs_si), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //   delete files(mValue);
                String[] uris = mValue.split(Pattern.quote(myPrefAttrsInfo.get_my_separator()));
                for(int i=0;i<uris.length;i++) Utils.delete_grx_icon_file_from_uri_string(uris[i]);
                // config default value
                mValue= myPrefAttrsInfo.get_my_string_def_value();
                config_preference(mValue);
                save_value(mValue);
                setSummary(mLabel);

            }
        });
        dlg.show();
    }


    private void config_preference(String valor){
        int napps=0;
        if(! (valor==null || valor.isEmpty())  ){
            String[] arr = valor.split(Pattern.quote(myPrefAttrsInfo.get_my_separator()));
            napps=arr.length;
        }
        if(napps==0) mLabel=myPrefAttrsInfo.get_my_summary();
        else mLabel= getContext().getString( R.string.gs_multi_accesos_seleccionadas,napps ) ;

    }


    public void GrxSetMultiValue(String value){
        if(!mValue.equals(value)){
            config_preference(value);
            save_value(value);
            mValue=value;
            setSummary(mLabel);
        }
    }

    /***************** just in case, to remember if in the future I add some functionality linked to custom dependency.... */

    @Override
    public void onDependencyChanged(Preference dependency, boolean disableDependent) {
        super.onDependencyChanged(dependency, disableDependent);
    }


    /********* broadcast , change group key value **********/

    private void send_broadcasts_and_change_group_key(){
        if(Common.SyncUpMode) return;
        GrxPreferenceScreen chl = (GrxPreferenceScreen) getOnPreferenceChangeListener();
        if(chl!=null){
            chl.send_broadcasts_and_change_group_key(myPrefAttrsInfo.get_my_group_key(),myPrefAttrsInfo.get_send_bc1(),myPrefAttrsInfo.get_send_bc2());
        }
    }

    /**********  Onpreferencechangelistener - add custom dependency rule *********/

    @Override
    public void setOnPreferenceChangeListener(Preference.OnPreferenceChangeListener onPreferenceChangeListener){
        if(!Common.SyncUpMode){
            super.setOnPreferenceChangeListener(onPreferenceChangeListener);
            String mydeprule = myPrefAttrsInfo.get_my_dependency_rule();
            if(mydeprule!=null){
                GrxPreferenceScreen grxPreferenceScreen = (GrxPreferenceScreen) getOnPreferenceChangeListener();
                grxPreferenceScreen.add_custom_dependency(this,mydeprule,null);
            }

        }else {
            GrxPreferenceScreen grxPreferenceScreen = (GrxPreferenceScreen) onPreferenceChangeListener;
            grxPreferenceScreen.add_group_key_for_syncup(myPrefAttrsInfo.get_my_group_key());
        }

    }

    /************ custom dependencies ****************/
    public void OnCustomDependencyChange(boolean state){
        setEnabled(state);
    }


}
