package cruzsoma.ai.cnn.fast_style_transfer_app;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import cruzsoma.ai.cnn.fast_style_transfer_app.Model;

import java.util.ArrayList;

public class StyleButtonAdapter extends RecyclerView.Adapter<StyleButtonAdapter.MyViewHolder> {
    private Context context;
    private ArrayList<Model> list;
    private ButtonInterface buttonInterface;
    private View lastSelected;

    public StyleButtonAdapter(Context context, ArrayList<Model> list) {
        this.context = context;
        this.list = list;
    }

    public void buttonSetOnclick(ButtonInterface buttonInterface){
        this.buttonInterface = buttonInterface;
    }

    public interface ButtonInterface{
        public void onclick(View view, Model model);
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        MyViewHolder holder = new MyViewHolder(LayoutInflater.from(
                context).inflate(R.layout.recycler_view_item, parent,
                false));
        return holder;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        holder.style_button.setImageResource(list.get(position).iconRes);
        holder.style_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(buttonInterface!=null) {
                    buttonInterface.onclick(v,list.get(position));
                }

                if (lastSelected != null) {
                    lastSelected.setBackgroundColor(Color.BLACK);
                }

                v.setBackgroundColor(Color.YELLOW);
                lastSelected = v;
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        ImageButton style_button;
        public MyViewHolder(View itemView) {
            super(itemView);
            style_button = (ImageButton) itemView.findViewById(R.id.styleButton);

        }
    }
}
