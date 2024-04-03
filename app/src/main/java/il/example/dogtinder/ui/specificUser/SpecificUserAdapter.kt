package il.example.dogtinder.ui.specificUser

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import il.example.dogtinder.databinding.CommentsLayoutBinding
import il.example.dogtinder.model.UserComments

class SpecificUserAdapter(private val currentLoggedIn:String, private val callback:deleteComment) : RecyclerView.Adapter<SpecificUserAdapter.SpecificViewHolder>() {

    private val list = ArrayList<UserComments>()

    fun setList(specific:Collection<UserComments>){
        list.clear()
        list.addAll(specific)
        notifyDataSetChanged()
    }

    interface deleteComment{
        fun deleteComment(comment:String,userWrote:String,dateComment:String)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpecificViewHolder {
        return SpecificViewHolder(CommentsLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: SpecificViewHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class SpecificViewHolder(private val binding:CommentsLayoutBinding) : RecyclerView.ViewHolder(binding.root){

        init {
            //Handle the click on the delete button in the fragment
            binding.deleteBtn.setOnClickListener {
                callback.deleteComment(list[adapterPosition].comment,list[adapterPosition].userWrote,list[adapterPosition].date)
            }
        }

        fun bind(){
            binding.commentOfUser.text = list[adapterPosition].comment
            binding.userCommentWrote.text = list[adapterPosition].userWrote
            binding.dateComment.text = list[adapterPosition].date
            //the visability of the delete button if the current logged in is the one that wrote the comment
            binding.deleteBtn.isVisible =currentLoggedIn.equals(list[adapterPosition].userWrote)

        }
    }
}