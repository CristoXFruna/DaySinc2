package com.example.daysinc.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.daysinc.R;
import com.example.daysinc.models.Task;

import java.util.List;

/**
 * Adaptador para gestionar y mostrar la lista de tareas (Task) en un RecyclerView.
 */
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    /**
     * Interfaz para escuchar los eventos de selección/cambio de estado en cada tarea.
     */
    public interface OnTaskClickListener {
        /**
         * Se ejecuta al hacer clic sobre una tarea para solicitar su cambio de estado.
         *
         * @param task        Objeto de la tarea interactuada.
         * @param position    Posición del elemento dentro de la lista.
         * @param targetState Nuevo estado sugerido (true para completado, false para pendiente).
         */
        void onTaskCheckChanged(Task task, int position, boolean targetState);
    }

    private final List<Task> taskList;
    private final OnTaskClickListener listener;

    /**
     * Constructor principal del adaptador.
     *
     * @param taskList Lista de tareas a renderizar.
     * @param listener Listener de eventos para la interacción con las tareas.
     */
    public TaskAdapter(List<Task> taskList, OnTaskClickListener listener) {
        this.taskList = taskList;
        this.listener = listener;
    }

    /**
     * Infla el diseño XML para cada fila de la lista y crea el ViewHolder.
     *
     * @param parent   ViewGroup padre al que se añadirá la nueva vista.
     * @param viewType Tipo de vista del elemento.
     * @return Nueva instancia de TaskViewHolder con la vista inflada.
     */
    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    /**
     * Vincula los datos de una tarea con sus respectivas vistas en la interfaz.
     *
     * @param holder   ViewHolder que contiene las referencias visuales.
     * @param position Posición del elemento en la lista de datos.
     */
    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.tvTitle.setText(task.getTitle());

        // Limpia el listener para evitar llamadas erróneas al reciclar la vista
        holder.cbCompleted.setOnCheckedChangeListener(null);
        holder.cbCompleted.setChecked(task.isCompleted());

        // Aplica o retira el tachado del texto según el estado de la tarea
        if (task.isCompleted()) {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }

        View.OnClickListener clickListener = v -> {
            // Mantiene el estado visual actual hasta ser verificado por MainActivity
            holder.cbCompleted.setChecked(task.isCompleted());
            if (listener != null) {
                listener.onTaskCheckChanged(task, holder.getAdapterPosition(), !task.isCompleted());
            }
        };

        holder.cbCompleted.setOnClickListener(clickListener);
        holder.itemView.setOnClickListener(clickListener);
    }

    /**
     * Obtiene el número total de tareas almacenadas en la lista.
     *
     * @return Cantidad de elementos de la lista o 0 si está vacía.
     */
    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }

    /**
     * Contenedor interno para gestionar las vistas de cada fila del RecyclerView.
     */
    static class TaskViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbCompleted;
        TextView tvTitle;

        /**
         * Constructor del ViewHolder.
         *
         * @param itemView Vista individual de la fila (item_task.xml).
         */
        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cbCompleted = itemView.findViewById(R.id.cbTaskCompleted);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
        }
    }
}