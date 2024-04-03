package il.example.dogtinder.ui.profile

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import il.example.dogtinder.databinding.AttendersLayoutBinding
import il.example.dogtinder.databinding.SearchedProfilesLayoutBinding
import il.example.dogtinder.model.User
import kotlinx.coroutines.flow.callbackFlow

class ProfilesAdapter(private val context: Context,private val callback:specificUser) : RecyclerView.Adapter<ProfilesAdapter.ProfileViewHolder>() {


    //we are not getting the list in the first place, we will update inside the fragment the list using the setList function when we observe for any changes in the
    //list of the users from the firestore, updating when Success is getting called
    private val list = ArrayList<User>()

    fun setList(users:Collection<User>){
        list.clear()
        list.addAll(users)
        notifyDataSetChanged()
    }

    interface specificUser{
        fun onUserClicked(user: User)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProfilesAdapter.ProfileViewHolder {
        return ProfileViewHolder(SearchedProfilesLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false,))
    }

    override fun onBindViewHolder(holder: ProfilesAdapter.ProfileViewHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount(): Int {
        return list.size
    }


    inner class ProfileViewHolder(private val binding : SearchedProfilesLayoutBinding) : RecyclerView.ViewHolder(binding.root){
        init {
            //when clicking on the whole line then we need to navigate to the specific user profile
            binding.root.setOnClickListener {
                callback.onUserClicked(list[adapterPosition])
            }
        }
        fun bind(){
            binding.profileName.text = list[adapterPosition].name
            binding.profileDogName.text = list[adapterPosition].dogsName
            binding.profilePhone.text = list[adapterPosition].phone

            Glide.with(context).load(list[adapterPosition].image).into(binding.profilePicture)
        }
    }
}