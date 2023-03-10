package com.github.kidplayer.view.adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.github.kidplayer.R;
import com.github.kidplayer.PlayerController;
import com.github.kidplayer.data.CatType;

import java.util.List;


public class FolderCatsListRecycleViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final RecyclerView recyclerView;
    private final FolderListAdapter moviesRecyclerViewAdapter;
    private Context mContext;
    private final LayoutInflater mLayoutInflater;

    private int curPos=0;

    public FolderCatsListRecycleViewAdapter(Context context, RecyclerView rv, FolderListAdapter moviesRecyclerViewAdapter) {
        this.mContext = context;
        this.recyclerView = rv;
        mLayoutInflater = LayoutInflater.from(mContext);
        this.moviesRecyclerViewAdapter=moviesRecyclerViewAdapter;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
       // View v = mLayoutInflater.inflate(R.layout.layout_recycleview_cats_item, parent, false);
       // view.setOnFocusChangeListener(mOnFocusChangeListener);
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_recycleview_cats_item, parent, false);
        return new RecyclerViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        final RecyclerViewHolder viewHolder = (RecyclerViewHolder) holder;

        CatType cat = PlayerController.getInstance().getCats().get(position);
        viewHolder.catText.setText(cat.getName());


        viewHolder.catText.setTextColor(cat.getName().equals( PlayerController.getInstance().getCurCat()) ? Color.RED : Color.WHITE);

        viewHolder.itemView.setFocusable(true);
        viewHolder.itemView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && !recyclerView.isComputingLayout()) {
                    PlayerController.getInstance().setCurCat(viewHolder.catText.getText().toString());
                    viewHolder.catText.setTextColor(Color.RED);
                    int p = curPos;
                    curPos = position;
                    notifyItemChanged(curPos);
                    notifyItemChanged(p);

                } else {
                }
            }});

        holder.itemView.setFocusable(true);

    }
    //?????????????????????????????????
    private void scrollToAmount(RecyclerView recyclerView, int dx, int dy) {
        //??????????????????????????????????????????????????????????????????????????????????????????
//                recyclerView.smoothScrollBy(dx,dy);

        //?????????????????????????????????
        try {
            Class recClass = recyclerView.getClass();
            recyclerView.smoothScrollBy(dx,dy);
            //Method smoothMethod = recClass.getDeclaredMethod("smoothScrollBy", int.class, int.class,  android.view.animation.Interpolator.class, int.class);
            //smoothMethod.invoke(recyclerView, dx, dy, new AccelerateDecelerateInterpolator(), 700);//???????????????700?????????
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    //????????????
    private void ofFloatAnimator(View view,float start,float end){
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(700);//????????????
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", start, end);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", start, end);
        animatorSet.setInterpolator(new DecelerateInterpolator());//?????????
        animatorSet.play(scaleX).with(scaleY);//????????????,????????????x???y?????????
        animatorSet.start();
    }

    /**
     * ???????????????????????????,?????????????????????????????????
     * @param recyclerView
     * @param view
     */
    private int[] getScrollAmount(RecyclerView recyclerView, View view) {
        int[] out = new int[2];
        final int parentLeft = recyclerView.getPaddingLeft();
        final int parentTop = recyclerView.getPaddingTop();
        final int parentRight = recyclerView.getWidth() - recyclerView.getPaddingRight();
        final int childLeft = view.getLeft() + 0 - view.getScrollX();
        final int childTop = view.getTop() + 0 - view.getScrollY();

        final int dx =childLeft - parentLeft - ((parentRight - view.getWidth()) / 2);//item???????????????Recyclerview????????????????????????????????????Recyclerview???????????????????????????

        final int dy = childTop - parentTop - (parentTop - view.getHeight()) / 2;//??????
        out[0] = dx;
        out[1] = dy;
        return out;

    }

    @Override
    public int getItemCount() {

        List<CatType> datas = PlayerController.getInstance().getCats();
        if (datas != null)
            return datas.size();
        return 0;
    }

    private class RecyclerViewHolder extends RecyclerView.ViewHolder {
        TextView catText;

        RecyclerViewHolder(View itemView) {
            super(itemView);
            catText = (TextView) itemView.findViewById(R.id.catText);
        }
    }
}
