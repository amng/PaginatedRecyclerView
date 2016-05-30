package pt.consipere.sample;

import java.util.ArrayList;
import java.util.List;

import pt.consipere.paginatedrecyclerview.Range;

public class ItemFeed {

    private static ItemFeed instance;
    private ItemFeed(){}

    public static ItemFeed getInstance(){
        return instance == null ? instance = new ItemFeed() : instance;
    }

    public List<Integer> getItemsRange(Range range){
        List<Integer> res = new ArrayList<>();
        try { //wait 2 seconds so we can see the progressbar
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (int i = range.from; i <= range.to; i++) {
            res.add(i);
        }
        return res;
    }

}
