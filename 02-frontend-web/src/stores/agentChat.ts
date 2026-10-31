import {ref} from 'vue'
export const agentMessages=ref<Record<string,any>[]>([])
export const agentSession=ref('')
export const agentPreviousSkill=ref('')
export const agentOwner=ref<number|undefined>()
