package net.noinspiration.idreader.activities;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import net.noinspiration.idreader.R;
import net.noinspiration.idreader.fragments.CategoryFragment;
import net.noinspiration.idreader.fragments.CertificateFragment;
import net.noinspiration.idreader.fragments.DocumentFragment;
import net.noinspiration.idreader.fragments.IdentityFragment;
import net.noinspiration.idreader.helper.AppProperties;
import net.noinspiration.idreader.helper.BACKeyHelper;
import net.noinspiration.idreader.helper.IdentityDocument;
import net.noinspiration.idreader.helper.Person;

import java.util.ArrayList;
import java.util.List;

public class PersonActivity extends AppCompatActivity {

    private String mrz;
    private BACKeyHelper bacKey;
    private Person person;
    private int callingActivity;
    private String docType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person);

        callingActivity = getIntent().getIntExtra("activity", -1);
        switch (callingActivity) {
            case AppProperties.ACTIVITY_DL_SCAN:
                mrz = getIntent().getStringExtra("mrz");
                break;
            case AppProperties.ACTIVITY_PASSPORT_SCAN:
                bacKey = getIntent().getParcelableExtra("backey");
                docType = getIntent().getStringExtra("doctype");
                break;
        }

        person = getIntent().getParcelableExtra("person");

        ViewPager viewPager = findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, NFCActivity.class);
        intent.putExtra("activity", callingActivity);
        switch (callingActivity) {
            case AppProperties.ACTIVITY_DL_SCAN:
                intent.putExtra("mrz", mrz);
                break;
            case AppProperties.ACTIVITY_PASSPORT_SCAN:
                intent.putExtra("backey", bacKey);
                intent.putExtra("doctype", docType);
                break;
        }
        startActivity(intent);
        finish();
    }

    public Person getPerson() {
        return person;
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new IdentityFragment(), getString(R.string.reading_info));
        if (callingActivity == AppProperties.ACTIVITY_DL_SCAN) {
            adapter.addFragment(new CategoryFragment(), getString(R.string.categories));
        }
        adapter.addFragment(new DocumentFragment(), getString(R.string.document));
        adapter.addFragment(new CertificateFragment(), getString(R.string.certificate));

        viewPager.setAdapter(adapter);
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }
}
