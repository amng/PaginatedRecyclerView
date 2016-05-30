package pt.consipere.paginatedrecyclerview;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collection;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;


/**
 * On the rest of the documentation of this file:
 *
 * Real position: is the real number of the items in memory, meaning that it will
 * always vary between 0 and {@link #pageSize} * {@link #maxPagesInMemory}
 *
 * Simulated position: is the real position + the offset which means that it may start at 0 or at
 * the offset that will be automatically calculated based on the current pages in memory. For example
 * if pageSize = 20 and maxPagesInMemory = 3 then for pages 1,2,3 meaning the first key of the map
 * will be 20, the position 0 will be 20 = 0 + 20 (offset) and the position 60 will be 60 + 20 = 80.
 *
 * @param <T> the type of adapter we are expecting
 */
public abstract class PaginatedAdapter<T> extends RecyclerView.Adapter {

    protected static final int LOADING_VIEW = Integer.MAX_VALUE;
    private NavigableMap<Integer, List<T>> rangeMap = new TreeMap<>();
    private int maxPagesInMemory = 3;
    private int pageSize = 20;
    private int loadingDirection = 0;
    private int layoutMoreProgress;

    /**
     * @param position the position of the item in the adapter
     * @return {@link #LOADING_VIEW} if the progressbar should appear, the default value otherwise
     */
    @Override
    public int getItemViewType(int position) {
        if (loadingDirection > 0 && position == getItemCount() - 1) {
            return LOADING_VIEW;

        } else if (loadingDirection < 0 && position == getOffset() - 1) {
            return LOADING_VIEW;

        }
        return super.getItemViewType(position);
    }

    /**
     * @return the number of items in memory plus the offset. If the list is loading some results
     * in the bottom direction, one more is added to the count to account for that position.
     *
     * The reason this is not the actual elements count is because this way we don't need to control
     * the scroll size nor do we need to scroll back when new results are added.
     */
    @Override
    public int getItemCount() {
        int loadingCount = loadingDirection > 0 ? 1 : 0;
        return rangeMap.size() > 0
                ? getRealCount() + rangeMap.firstKey() + loadingCount
                : getRealCount() + loadingCount;
    }

    /**
     * @return The real number of elements in the adapter.
     */
    public int getRealCount(){
        int count = 0;
        for (List<T> list : rangeMap.values()) {
            count += list.size();
        }
        return count;
    }

    /**
     * @param position the real position that is given by {@link #onBindPageViewHolder}
     * @return the item at the specified position.
     */
    public T getItem(int position){
        int index = getOffset() + ((int) Math.floor(position/pageSize) * pageSize);
        return rangeMap.get(index).get(position % pageSize);
    }

    /**
     * Function that's responsible for injecting the progressbar layout  in one of the views
     * Note: This method should not be used!
     *       Use {@link #onCreatePageViewHolder(ViewGroup, int)} instead.
     */
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == LOADING_VIEW) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(layoutMoreProgress, parent, false);
            return new RecyclerView.ViewHolder(view) {};
        }
        return onCreatePageViewHolder(parent, viewType);
    }

    /**
     * This function is responsible for setting up the viewHolder with the real position instead of
     * the simulated one.
     *
     * Note: This method should not be used!
     *       Use {@link #onBindPageViewHolder(RecyclerView.ViewHolder, int)} instead.
     *
     * @param position the simulated position
     */
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int paginatedPosition = position - getOffset();
        if (paginatedPosition > -1) {
            onBindPageViewHolder(holder, paginatedPosition);
        }
    }

    /**
     * @param elements the elements that are to be added when the application finishes loading them.
     *                  This function will add the elements to the end or to the bottom of the map
     *                  based on the loading direction.
     */
    public void addElements(List<T> elements){
        int pageStart = 0;

        if (rangeMap.size() > 0) {
            if (loadingDirection == RecyclerViewScrollListener.LOAD_DOWN) {
                pageStart = rangeMap.lastKey() + pageSize;

            } else {
                pageStart = rangeMap.firstKey() - pageSize;
            }
        }
        rangeMap.put(pageStart, elements);
    }

    /**
     * @return A collection of lists. Each list represents one of the pages in memory
     */
    public Collection<List<T>> getAllPages(){
        return rangeMap.values();
    }

    /**
     * Clear the colleciton of lists (should be called when refreshing the list)
     */
    public void clearAll() {
        rangeMap.clear();
    }

    public void setMaxPagesInMemory(int maxPagesInMemory) {
        this.maxPagesInMemory = maxPagesInMemory;
    }

    /**
     * @param direction the direction of the next range, should be one of
     *                  {@link RecyclerViewScrollListener#LOAD_DOWN} or
     *                  {@link RecyclerViewScrollListener#LOAD_UP}
     *
     * @return the next range of items to be fetched. For example if the adapter has got the keys
     *         0 and 20 ({@link #pageSize} = 20), it means it has 40 results, therefore the next
     *         range will be [40, 59].
     *         If there are still no elements in the adapter, then the direction is not considered
     *         and the returned range will be [0 - {@link #pageSize}].
     *         If no direction is provided {@link RecyclerViewScrollListener#LOAD_UP} will be used
     *         by default.
     */
    public Range getNextRange(int direction) {
        int from, to;
        if (rangeMap.size() == 0) {
            return new Range(0, pageSize - 1);
        }

        if (direction == RecyclerViewScrollListener.LOAD_DOWN) {
            from = rangeMap.lastKey() + pageSize;
            to = from + pageSize - 1;
            return new Range(from, to);

        } else {
            to = rangeMap.firstKey() - 1;
            from = to - (pageSize - 1);
            return new Range(from, to);
        }
    }

    /**
     * @return the offset of the first page in memory. For example if the adapter has
     *         {@link #pageSize} = 20 and the pages in memory are 20, 40, 60, then the offset will
     *         be 20, meaning that we have all the items from 20 to 79.
     */
    public int getOffset() {
        return rangeMap.size() > 0 ? rangeMap.firstKey() : 0;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getPageSize() {
        return pageSize;
    }

    /**
     * @param direction the direction in which the loading will occur, should be one of
     *                  {@link RecyclerViewScrollListener#LOAD_DOWN} or
     *                  {@link RecyclerViewScrollListener#LOAD_UP}
     *
     * Sets the loading direction and removes a page in the opposite direction if the number of pages
     * plus one exceeds the maxPagesInMemory. This way some scroll issues are avoided when the list
     * is loading the items in one direction but the user scrolls all the way in the other direction.
     */
    public void setLoadingDirection(int direction){
        this.loadingDirection = direction;
        if (direction != RecyclerViewScrollListener.NOT_LOADING) {
            if (rangeMap.size() + 1 > maxPagesInMemory) {
                if (loadingDirection == RecyclerViewScrollListener.LOAD_DOWN) {
                    rangeMap.remove(rangeMap.firstKey());

                } else {
                    rangeMap.remove(rangeMap.lastKey());
                }
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Method that should be used to setup the viewHolder
     * @param position the real position of the item in the adapter
     */
    public abstract void onBindPageViewHolder(RecyclerView.ViewHolder holder, int position);

    public abstract RecyclerView.ViewHolder onCreatePageViewHolder(ViewGroup parent, int viewType);

    public void setLayoutMoreProgress(int layoutMoreProgress) {
        this.layoutMoreProgress = layoutMoreProgress;
    }
}
