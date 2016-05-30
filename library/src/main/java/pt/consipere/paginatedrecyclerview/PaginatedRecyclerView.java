package pt.consipere.paginatedrecyclerview;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.malinskiy.superrecyclerview.SuperRecyclerView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PaginatedRecyclerView extends SuperRecyclerView {

    private int pageSize = 20;
    private int maxPagesInMemory = 3;
    private int loadOffset = 0;
    private int loadingDirection = 0;
    private int layoutMoreProgress = -1;

    private RecyclerViewScrollListener listener;

    private OnLoadMoreTopListener loadMoreTopListener;
    private OnLoadMoreBottomListener loadMoreBottomListener;
    private boolean isRefreshing = false;

    public PaginatedRecyclerView(Context context) {
        super(context);
        setLayoutManager();
    }

    public PaginatedRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        parseAttributes(context, attrs);
        setLayoutManager();
    }

    public PaginatedRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        parseAttributes(context, attrs);
        setLayoutManager();
    }

    /**
     * Setup the scrollListener in order to automatically load the next batch of items
     */
    private void setListeners() {
        if (getRecyclerView().getLayoutManager() instanceof LinearLayoutManager) {
            listener = new RecyclerViewScrollListener((LinearLayoutManager)
                    getRecyclerView().getLayoutManager()) {
                @Override
                public void onLoadMore(int direction) {
                    loadingDirection = direction;
                    if (direction == RecyclerViewScrollListener.LOAD_DOWN &&
                            loadMoreBottomListener != null) {
                        loadMoreBottomListener.onLoadMore();
                    } else if (direction == RecyclerViewScrollListener.LOAD_UP &&
                            loadMoreTopListener != null) {
                        loadMoreTopListener.onLoadMore();
                    }
                }
            };
            listener.setLoadOffset(loadOffset);

            setOnScrollListener(listener);
        } else {
            Log.e(this.getClass().getSimpleName(),
                    "Layout Manager is not instance of LinearLayoutManager");
        }
    }

    @Override
    public void setRefreshing(boolean refreshing) {
        isRefreshing = refreshing;
        listener.setRefreshing(refreshing);
        super.setRefreshing(refreshing);
    }

    public boolean isRefreshing() {
        return isRefreshing;
    }

    /**
     * Parse the attributes that are set on the xml
     */
    private void parseAttributes(Context context, AttributeSet attrs){
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.PaginatedRecyclerView,
                0, 0);
        try {
            pageSize = a.getInteger(R.styleable.PaginatedRecyclerView_pageSize, 20);
            maxPagesInMemory = a.getInteger(R.styleable.PaginatedRecyclerView_maxPagesInMem, 3);
            loadOffset = a.getInteger(R.styleable.PaginatedRecyclerView_loadOffset, 0);
            layoutMoreProgress = a.getResourceId(R.styleable.PaginatedRecyclerView_layoutMoreProgress,
                    R.layout.row_progress);
            final String loadUp = a.getString(R.styleable.PaginatedRecyclerView_onLoadMoreUp);
            final String loadDown = a.getString(R.styleable.PaginatedRecyclerView_onLoadMoreDown);

            if ((loadDown != null || loadUp != null) && context.isRestricted()) {
                throw new IllegalStateException("The app:OnLoadMoreUp or app:OnLoadMoreDown" +
                        " attribute cannot be used within a restricted context");
            }

            if (loadDown != null) {
                setLoadMoreBottomListener(new DeclaredOnLoadMoreListener(this, loadDown, "onLoadMoreDown"));
            }

            if (loadUp != null ) {
                setLoadMoreTopListener(new DeclaredOnLoadMoreListener(this, loadUp, "onLoadMoreUp"));
            }

        } finally {
            a.recycle();
        }
    }

    /**
     * @param adapter the PaginatedAdapter (trying to set an adapter that is not instance of
     *                paginated adapter won't set the adapter and an error message will appear in
     *                the log.
     */
    @Override
    public void setAdapter(RecyclerView.Adapter adapter) {
        if (adapter instanceof PaginatedAdapter) {
            ((PaginatedAdapter)adapter).setPageSize(pageSize);
            ((PaginatedAdapter)adapter).setMaxPagesInMemory(maxPagesInMemory);
            ((PaginatedAdapter)adapter).setLayoutMoreProgress(layoutMoreProgress);
            super.setAdapter(adapter);

        } else {
            Log.e(this.getClass().getSimpleName(), "Adapter is not instance of Paginated Adapter");
        }
    }

    /**
     * @param manager this method should not be used! Setting the manager here won't work, since the
     *                layout manager will be set automatically because of the overrides that must be
     *                done in order to avoid scrolling disasters!
     */
    @Override
    public void setLayoutManager(RecyclerView.LayoutManager manager) {
        Log.e(this.getClass().getSimpleName(), "Layout Manager will be set automatically");
    }

    /**
     * Sets the custom LinearLayoutManager with the overridden methods that will control the scroll
     * limits when the user is scrolling up.
     */
    private void setLayoutManager(){
        super.setLayoutManager(new LinearLayoutManager(getContext()) {

            @Override
            public void onScrollStateChanged(int state) {
                PaginatedAdapter adapter = (PaginatedAdapter) getAdapter();
                int firstVisibleItem = findFirstVisibleItemPosition();
                int firstCompleteVisible = findFirstCompletelyVisibleItemPosition();
                int rangeMapOffset = adapter.getOffset() - getLoadingOffset();
                /* Check if the first visible position is lower then the adapter offset. This might
                 *  happen if the scrollVerticallyBy dy is higher then max scroll needed to get to
                 *  the offset position. In this case just scroll back to the offset position
                 */
                if (firstVisibleItem < rangeMapOffset || firstCompleteVisible < rangeMapOffset) {
                    View v = findViewByPosition(rangeMapOffset);
                    LinearLayoutManager layoutManager =
                            ((LinearLayoutManager)getRecyclerView().getLayoutManager());
                    if (v != null) {
                        layoutManager.scrollToPositionWithOffset(rangeMapOffset, 0);

                    } else {
                        layoutManager.scrollToPositionWithOffset(rangeMapOffset, 0);
                    }
                }
            }


            @Override
            public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
                PaginatedAdapter adapter = (PaginatedAdapter) getAdapter();
                int firstVisibleItem = findFirstCompletelyVisibleItemPosition();
                int rangeMapOffset = adapter.getOffset() - getLoadingOffset();

                View v = findViewByPosition(rangeMapOffset);

                //don't scroll anything from this point if scrolling up and the list first visible
                //item is the adapter offset
                if (firstVisibleItem == rangeMapOffset && rangeMapOffset > -1 && dy < 0) {
                    return 0;
                }

                //if the first visible position equals the adapter offset but it is not completely
                //visible, then the list will scroll to the position with that offset
                if (v != null && findFirstVisibleItemPosition() == rangeMapOffset && dy < 0) {
                    LinearLayoutManager layoutManager =
                            ((LinearLayoutManager)getRecyclerView().getLayoutManager());
                    layoutManager.scrollToPositionWithOffset(rangeMapOffset, 0);
                }

                //just scroll normally
                return super.scrollVerticallyBy(dy, recycler, state);
            }

            /**
             * @return 1 if the progressbar is being shown when scrolling up, 0 otherwise
             */
            private int getLoadingOffset(){
                if (isLoading() && loadingDirection == RecyclerViewScrollListener.LOAD_UP) {
                    return 1;

                } else {
                    return 0;
                }
            }
        });

        setListeners();
    }

    public OnLoadMoreTopListener getLoadMoreTopListener() {
        return loadMoreTopListener;
    }

    public void setLoadMoreTopListener(OnLoadMoreTopListener loadMoreTopListener) {
        this.loadMoreTopListener = loadMoreTopListener;
    }

    public OnLoadMoreBottomListener getLoadMoreBottomListener() {
        return loadMoreBottomListener;
    }

    public void setLoadMoreBottomListener(OnLoadMoreBottomListener loadMoreBottomListener) {
        this.loadMoreBottomListener = loadMoreBottomListener;
    }

    public int getPageSize() {
        return pageSize;
    }

    public boolean isLoading() {
        return listener.isLoading();
    }

    public void setLoading(boolean loading) {
        listener.setLoading(loading);
    }

    /**
     * Interfaces that will be called when new results should be loaded
     */
    public interface OnLoadMoreTopListener {
        void onLoadMore();
    }

    public interface OnLoadMoreBottomListener {
        void onLoadMore();
    }

    /*
    Code taken from Android View and modified accordingly to the needs
    This class allows to invoke a function by it's name. This class will be used to allow the
    use of the function names on xml just like the onClick for the views.
     */
    private static class DeclaredOnLoadMoreListener implements OnLoadMoreTopListener,
            OnLoadMoreBottomListener {
        private final View hostView;
        private final String methodName;
        private final String listenerName;

        private Method method;

        public DeclaredOnLoadMoreListener(@NonNull View hostView, @NonNull String methodName,
                                          @NonNull String listenerName) {
            this.hostView = hostView;
            this.methodName = methodName;
            this.listenerName = listenerName;
        }

        @Override
        public void onLoadMore() {
            if (method == null) {
                method = resolveMethod(hostView.getContext(), methodName);
            }

            try {
                method.invoke(hostView.getContext());

            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Could not execute non-public method for app:"
                        + listenerName, e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException("Could not execute method for app:"+listenerName, e);
            }
        }

        @NonNull
        private Method resolveMethod(@Nullable Context context, @NonNull String name) {
            while (context != null) {
                try {
                    if (!context.isRestricted()) {
                        return context.getClass().getMethod(name);
                    }
                } catch (NoSuchMethodException e) {
                    // Failed to find method, keep searching up the hierarchy.
                }

                if (context instanceof ContextWrapper) {
                    context = ((ContextWrapper) context).getBaseContext();
                } else {
                    // Can't search up the hierarchy, null out and fail.
                    context = null;
                }
            }

            final int id = hostView.getId();
            final String idText = id == NO_ID ? "" : " with id '"
                    + hostView.getContext().getResources().getResourceEntryName(id) + "'";
            throw new IllegalStateException("Could not find method " + name
                    + "() in a parent or ancestor Context for app:" + listenerName
                    + " attribute defined on view " + hostView.getClass() + idText);
        }
    }

}
