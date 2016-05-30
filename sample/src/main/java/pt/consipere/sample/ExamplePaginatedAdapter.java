package pt.consipere.sample;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import pt.consipere.paginatedrecyclerview.PaginatedAdapter;

public class ExamplePaginatedAdapter extends PaginatedAdapter<Integer> {
    @Override
    public void onBindPageViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ExampleViewHolder) {
            ((ExampleViewHolder) holder).setupHolder(getItem(position));
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreatePageViewHolder(ViewGroup parent, int viewType) {
        return new ExampleViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_example, parent, false));
    }

    public static class ExampleViewHolder extends RecyclerView.ViewHolder{

        private TextView t;

        public ExampleViewHolder(View itemView) {
            super(itemView);
            t = (TextView) itemView.findViewById(R.id.text);
        }

        public void setupHolder(int number){
            t.setText(t.getContext().getString(R.string.item_number, number));
        }
    }
}
