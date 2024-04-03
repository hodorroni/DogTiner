package il.example.dogtinder.ui.attenders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import il.example.dogtinder.databinding.AttendersLayoutBinding
import il.example.dogtinder.model.Event

class AttendersAdapter(private val list:List<String>) : RecyclerView.Adapter<AttendersAdapter.AttendersViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendersViewHolder {
        return AttendersViewHolder(AttendersLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false,))
    }

    override fun onBindViewHolder(holder: AttendersViewHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class AttendersViewHolder(private val binding:AttendersLayoutBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(){
            binding.attendersName.text = "${adapterPosition+1}."+list[adapterPosition]
        }
    }



}