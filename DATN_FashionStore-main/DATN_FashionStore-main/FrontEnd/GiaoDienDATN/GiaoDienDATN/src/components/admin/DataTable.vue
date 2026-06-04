<template>
  <div class="data-table">
    <table>
      <thead>
        <tr>
          <th v-if="selectable" width="40">
            <input 
              type="checkbox" 
              :checked="isAllSelected"
              @change="$emit('select-all', $event.target.checked)"
            >
          </th>
          <th v-for="col in columns" :key="col.key">
            {{ col.label }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, index) in data" :key="index">
          <td v-if="selectable">
            <input 
              type="checkbox" 
              :value="row.id"
              :checked="isSelected(row.id)"
              @change="$emit('select', row.id, $event.target.checked)"
            >
          </td>
          <td v-for="col in columns" :key="col.key">
            <!-- Custom cell template -->
            <slot 
              v-if="$slots[`cell-${col.key}`]" 
              :name="`cell-${col.key}`" 
              :row="row"
            />
            <!-- Formatted value -->
            <template v-else-if="col.format">
              {{ col.format(row[col.key]) }}
            </template>
            <!-- Default value -->
            <template v-else>
              {{ row[col.key] }}
            </template>
          </td>
        </tr>
        <tr v-if="data.length === 0">
          <td :colspan="columnCount" class="no-data">
            Không có dữ liệu
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  columns: {
    type: Array,
    required: true
  },
  data: {
    type: Array,
    required: true
  },
  selectable: {
    type: Boolean,
    default: false
  },
  selected: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['select', 'select-all'])

const columnCount = computed(() => {
  let count = props.columns.length
  if (props.selectable) count += 1
  return count
})

const isAllSelected = computed(() => {
  return props.data.length > 0 && 
         props.data.every(row => props.selected.includes(row.id))
})

const isSelected = (id) => {
  return props.selected.includes(id)
}
</script>

<style scoped>
.data-table {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th {
  background: var(--light-gray);
  padding: 15px;
  text-align: left;
  font-weight: 600;
  color: var(--dark-gray);
  border-bottom: 2px solid var(--border-color);
}

td {
  padding: 15px;
  border-bottom: 1px solid var(--border-color);
  color: var(--medium-gray);
}

tr:hover {
  background: var(--light-gray);
}

.no-data {
  text-align: center;
  padding: 40px;
  color: var(--medium-gray);
}

input[type="checkbox"] {
  cursor: pointer;
  accent-color: var(--primary-red);
}
</style>