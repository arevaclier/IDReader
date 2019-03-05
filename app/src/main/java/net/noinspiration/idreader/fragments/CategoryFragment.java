package net.noinspiration.idreader.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.noinspiration.idreader.R;
import net.noinspiration.idreader.activities.PersonActivity;
import net.noinspiration.idreader.helper.IdentityDocument;
import net.noinspiration.idreader.helper.Person;
import net.noinspiration.idreader.identitydocument.DriverLicenseCategory;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;


public class CategoryFragment extends Fragment {

    private final static String TAG = "CategoryFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        LinearLayout linearLayout = view.findViewById(R.id.categoryLayout);

        PersonActivity activity = (PersonActivity) getActivity();
        Person person = activity.getPerson();
        IdentityDocument document = person.getIdentityDocument();
        List<DriverLicenseCategory> categoryList = document.getDriverLicenseCategories();
        for(DriverLicenseCategory category : categoryList) {
            View v = createCategoryView(view, category.getCategory());
            linearLayout.addView(v);

            TextView issueDate = v.findViewById(R.id.catIssueDateText);
            TextView expDate = v.findViewById(R.id.catExpDateText);
            TextView extra = v.findViewById(R.id.catExtraText);

            issueDate.setText(category.getIssueDate());
            expDate.setText(category.getExpDate());
            if(!category.getExtra().equals(""))
                extra.setText(category.getExtra());
        }
    }

    private View createCategoryView(View view, String categoryCode) {
        View v;
        LayoutInflater inflater = (LayoutInflater) view.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = inflater.inflate(R.layout.include_category, null);

        TextView categoryTitle = v.findViewById(R.id.categoryTitle);
        categoryTitle.setText(getString(R.string.category, categoryCode));

        return v;
    }
}
