package pt.consipere.paginatedrecyclerview;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public abstract class RecyclerViewScrollListener extends RecyclerView.OnScrollListener {

    private int loadOffset = 10;
    public final static int LOAD_UP = -1;
    public final static int LOAD_DOWN = 1;
    public final static int NOT_LOADING = 0;

    private LinearLayoutManager layoutManager;
    private PaginatedAdapter adapter = null;
    private boolean loading = false;
    private boolean refreshing;

    public RecyclerViewScrollListener(LinearLayoutManager layoutManager) {
        this.layoutManager = layoutManager;
    }

    @Override
    public void onScrolled(RecyclerView view, int dx, int dy) {

        adapter = (PaginatedAdapter) view.getAdapter();
        int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
        int lastVisibleItem = layoutManager.findFirstVisibleItemPosition() + view.getChildCount();
        int totalItemCount = layoutManager.getItemCount();

        /**
         * if the dy make the list scroll past the offset position upwards, then stop scroll as soon
         * as the offset position is passed (the position will then be fixed by
         * {@link PaginatedRecyclerView#setLayoutManager()}
         */
        if (layoutManager.findFirstCompletelyVisibleItemPosition() < adapter.getOffset()) {
            view.stopScroll();
            view.stopNestedScroll();
        }

        //Calculate when the next items need to be loaded
        if (!loading && !refreshing) {
            //scroll up
            if (dy < 0 && firstVisibleItem <= loadOffset + adapter.getOffset() &&
                    adapter.getOffset() > 0) {
                adapter.setLoadingDirection(LOAD_UP);
                onLoadMore(LOAD_UP);
                loading = true;
            }
            //scroll down
            else if (dy > 0 && ((totalItemCount - lastVisibleItem) <= loadOffset)) {
                adapter.setLoadingDirection(LOAD_DOWN);
                onLoadMore(LOAD_DOWN);
                loading = true;
            }
        }
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
        if (!loading && adapter != null) {
            adapter.setLoadingDirection(NOT_LOADING);
        }
    }

    public boolean isLoading(){
        return this.loading;
    }

    public void setLoadOffset(int loadOffset){
        this.loadOffset = loadOffset;
    }

    public abstract void onLoadMore(int direction);

    public void setRefreshing(boolean refreshing) {
        this.refreshing = refreshing;
    }
}
