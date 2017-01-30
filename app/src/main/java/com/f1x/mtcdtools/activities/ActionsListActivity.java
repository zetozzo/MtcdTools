package com.f1x.mtcdtools.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.f1x.mtcdtools.ActionsList;
import com.f1x.mtcdtools.R;
import com.f1x.mtcdtools.adapters.KeysSequenceArrayAdapter;
import com.f1x.mtcdtools.adapters.NamesArrayAdapter;
import com.f1x.mtcdtools.input.KeysSequenceConverter;
import com.f1x.mtcdtools.storage.exceptions.DuplicatedEntryException;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

public class ActionsListActivity extends ServiceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actions_list_details);

        // -----------------------------------------------------------------------------------------

        mKeysSequenceUpArrayAdapter = new KeysSequenceArrayAdapter(this);
        ListView keysSequenceUpListView = (ListView)this.findViewById(R.id.listViewKeysSequenceUp);
        keysSequenceUpListView.setAdapter(mKeysSequenceUpArrayAdapter);

        Button obtainKeysSequenceUpButton = (Button)this.findViewById(R.id.buttonObtainKeysSequenceUp);
        obtainKeysSequenceUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(ActionsListActivity.this, ObtainKeysSequenceActivity.class), REQUEST_CODE_KEYS_SEQUENCE_UP);
            }
        });

        // -----------------------------------------------------------------------------------------

        mKeysSequenceDownArrayAdapter = new KeysSequenceArrayAdapter(this);
        ListView keysSequenceDownListView = (ListView)this.findViewById(R.id.listViewKeysSequenceDown);
        keysSequenceDownListView.setAdapter(mKeysSequenceDownArrayAdapter);

        Button obtainKeysSequenceDownButton = (Button)this.findViewById(R.id.buttonObtainKeysSequenceDown);
        obtainKeysSequenceDownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(ActionsListActivity.this, ObtainKeysSequenceActivity.class), REQUEST_CODE_KEYS_SEQUENCE_DOWN);
            }
        });

        // -----------------------------------------------------------------------------------------

        mActionsNamesArrayAdapter = new NamesArrayAdapter(this);
        final Spinner actionsSpinner = (Spinner)this.findViewById(R.id.spinnerActions);
        actionsSpinner.setAdapter(mActionsNamesArrayAdapter);

        ListView addedActionsListView = (ListView)this.findViewById(R.id.listViewActions);
        mAddedActionsNamesArrayAdapter = new NamesArrayAdapter(this);
        addedActionsListView.setAdapter(mAddedActionsNamesArrayAdapter);
        addedActionsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                String actionName = mAddedActionsNamesArrayAdapter.getItem(position);
                mAddedActionsNamesArrayAdapter.remove(actionName);

                return true;
            }
        });

        Button addActionButton = (Button)this.findViewById(R.id.buttonAddAction);
        addActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String actionName = (String)actionsSpinner.getSelectedItem();

                if(!mAddedActionsNamesArrayAdapter.containsItem(actionName)) {
                    mAddedActionsNamesArrayAdapter.add(actionName);
                } else {
                    Toast.makeText(ActionsListActivity.this, ActionsListActivity.this.getText(R.string.ObjectAlreadyAdded), Toast.LENGTH_LONG).show();
                }
            }
        });

        // -----------------------------------------------------------------------------------------

        mEditActionsListName = this.getIntent().getStringExtra(ACTIONS_LIST_NAME_PARAMETER);
        mEditMode = mEditActionsListName != null;

        mActionsListNameEditText = (EditText)this.findViewById(R.id.editTextActionsListName);

        // -----------------------------------------------------------------------------------------

        Button cancelButton = (Button)this.findViewById(R.id.buttonCancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActionsListActivity.this.finish();
            }
        });

        Button saveButton = (Button)this.findViewById(R.id.buttonSave);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String actionsListName = mActionsListNameEditText.getText().toString();

                if(!actionsListName.isEmpty()) {
                    storeActionsList(actionsListName);
                } else {
                    Toast.makeText(ActionsListActivity.this, ActionsListActivity.this.getText(R.string.EmptyNameError), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void storeActionsList(String actionsListName) {
        try {
            ActionsList actionsList = new ActionsList(actionsListName,
                    mKeysSequenceUpArrayAdapter.getItems(),
                    mKeysSequenceDownArrayAdapter.getItems(),
                    mAddedActionsNamesArrayAdapter.getItems());

            if(mEditMode) {
                mServiceBinder.getActionsListsStorage().replace(mEditActionsListName, actionsListName, actionsList);
            } else {
                mServiceBinder.getActionsListsStorage().insert(actionsListName, actionsList);
            }

            finish();
        } catch (DuplicatedEntryException e) {
            e.printStackTrace();
            Toast.makeText(this, this.getText(R.string.ObjectAlreadyAdded), Toast.LENGTH_LONG).show();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onServiceConnected() {
        if(mEditMode) {
            ActionsList actionsList = mServiceBinder.getActionsListsStorage().getItem(mEditActionsListName);

            if(actionsList != null) {
                mAddedActionsNamesArrayAdapter.reset(actionsList.getActionNames());
                mKeysSequenceDownArrayAdapter.reset(actionsList.getKeysSequenceDown());
                mKeysSequenceUpArrayAdapter.reset(actionsList.getKeysSequenceUp());
                mActionsListNameEditText.setText(actionsList.getName());
            }
        }

        mActionsNamesArrayAdapter.reset(mServiceBinder.getActionsStorage().getItems().keySet());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == ObtainKeysSequenceActivity.RESULT_CANCELED) {
            return;
        }

        List<Integer> keysSequence = KeysSequenceConverter.fromArray(data.getIntArrayExtra(ObtainKeysSequenceActivity.RESULT_NAME));

        if(requestCode == REQUEST_CODE_KEYS_SEQUENCE_UP) {
            mKeysSequenceUpArrayAdapter.reset(keysSequence);
        } else if(requestCode == REQUEST_CODE_KEYS_SEQUENCE_DOWN) {
            mKeysSequenceDownArrayAdapter.reset(keysSequence);
        }
    }

    private String mEditActionsListName;
    private boolean mEditMode;

    EditText mActionsListNameEditText;
    KeysSequenceArrayAdapter mKeysSequenceUpArrayAdapter;
    KeysSequenceArrayAdapter mKeysSequenceDownArrayAdapter;
    NamesArrayAdapter mActionsNamesArrayAdapter;
    NamesArrayAdapter mAddedActionsNamesArrayAdapter;

    public static final String ACTIONS_LIST_NAME_PARAMETER = "actionName";
    private static int REQUEST_CODE_KEYS_SEQUENCE_UP = 100;
    private static int REQUEST_CODE_KEYS_SEQUENCE_DOWN = 101;
}