/**
 * Copyright 2014 Gustavo Martin Morcuende
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package name.gumartinm.weather.information.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import name.gumartinm.weather.information.R;

public class APIKeyNoticeDialogFragment extends DialogFragment {

    public static APIKeyNoticeDialogFragment newInstance(final int title) {
        final APIKeyNoticeDialogFragment frag = new APIKeyNoticeDialogFragment();
        final Bundle args = new Bundle();

        args.putInt("title", title);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final int title = this.getArguments().getInt("title");

        return new AlertDialog.Builder(this.getActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(this.getString(R.string.api_id_key_notice_message))
                .setPositiveButton(this.getString(R.string.api_id_key_notice_ok_button), null)
                .create();
    }
    
    @Override
    public void onDestroyView() {
    	if (getDialog() != null && getRetainInstance()) {
    		getDialog().setDismissMessage(null);
    	}
    	super.onDestroyView();
    }
}