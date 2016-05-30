package pt.consipere.sample;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;

import java.util.List;

import pt.consipere.paginatedrecyclerview.PaginatedRecyclerView;
import pt.consipere.paginatedrecyclerview.Range;

import static pt.consipere.paginatedrecyclerview.RecyclerViewScrollListener.LOAD_DOWN;
import static pt.consipere.paginatedrecyclerview.RecyclerViewScrollListener.LOAD_UP;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener{

    PaginatedRecyclerView paginatedRecyclerView = null;
    ExamplePaginatedAdapter adapter = new ExamplePaginatedAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        paginatedRecyclerView = (PaginatedRecyclerView) findViewById(R.id.list);
        assert paginatedRecyclerView != null;
        paginatedRecyclerView.setAdapter(adapter);
        paginatedRecyclerView.setRefreshListener(this);
    }

    public void loadMoreDown() {
        new LoadItems(adapter.getNextRange(LOAD_DOWN)).execute();
    }

    public void loadMoreUp() {
        new LoadItems(adapter.getNextRange(LOAD_UP)).execute();
    }

    @Override
    public void onRefresh() {
        adapter.clearAll();
        new LoadItems(adapter.getNextRange(LOAD_DOWN)).execute();
    }

    private class LoadItems extends AsyncTask<Void, Void, Void> {
        private Range range;

        public LoadItems(Range range){
            this.range = range;
        }

        @Override
        protected Void doInBackground(Void... params) {
            List<Integer> items = ItemFeed.getInstance().getItemsRange(range);
            adapter.addElements(items);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            paginatedRecyclerView.setRefreshing(false);
            paginatedRecyclerView.setLoading(false);
            adapter.notifyDataSetChanged();
        }
    }
}
