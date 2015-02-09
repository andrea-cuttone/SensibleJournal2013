package dk.dtu.imm.sensible;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.Window;

import com.viewpagerindicator.TabPageIndicator;

import dk.dtu.imm.sensible.bt.BtFragment_;
import dk.dtu.imm.sensible.btnetwork.BtNetworkFragment_;
import dk.dtu.imm.sensible.components.CustomFragment;
import dk.dtu.imm.sensible.components.UILogger;
import dk.dtu.imm.sensible.movement.MovementViewerFragment;
import dk.dtu.imm.sensible.stats.StatsFragment_;
import dk.dtu.imm.sensible.timespiral.TimeSpiralFragment_;
import dk.dtu.imm.sensiblejournal.R;

public class MainTabsActivity extends FragmentActivity {
    
	private CustomFragment frags[];
	private ViewPager pager;    
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Constants.APP_NAME, "****************");
        Log.d(Constants.APP_NAME, "*** STARTING ***");
        Log.d(Constants.APP_NAME, "****************");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main_tabs); 

        frags = new CustomFragment [] { new BtNetworkFragment_(), 
        								new TimeSpiralFragment_(), 
        								new MovementViewerFragment(), 
        								new BtFragment_(), 
        								new StatsFragment_(), 
        								new AboutFragment_() 
        							  };

        FragmentPagerAdapter adapter = new CustomPagerAdapter(getSupportFragmentManager());

        pager = (ViewPager)findViewById(R.id.tabs_pager);
        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(1);
        
        TabPageIndicator indicator = (TabPageIndicator)findViewById(R.id.tabs_indicator);
        indicator.setViewPager(pager);
        indicator.setOnPageChangeListener(new OnPageChangeListener() {
			
			@Override
			public void onPageSelected(int selected) {
				notifyTabSelection(selected);
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) { }
			
			@Override
			public void onPageScrollStateChanged(int arg0) { }
		});
    }
	
	public void notifyTabSelection(int selected) {
		for (int i = 0; i < frags.length; i++) {
			if(i == selected) {
				frags[i].onTabSelected();
			} else {
				frags[i].onTabUnselected();
			}
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		notifyTabSelection(pager.getCurrentItem());
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		UILogger.instance(getApplicationContext()).logEvent("paused");
		notifyTabSelection(-1);
	}
    
    class CustomPagerAdapter extends FragmentPagerAdapter {
        public CustomPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
        	return frags[position];
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return frags[position].getTabTitle().toUpperCase();
        }

        @Override
        public int getCount() {
          return frags.length;
        }
    }
}
