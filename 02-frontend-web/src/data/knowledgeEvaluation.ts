export const evaluationFiles=[
 {key:'process',id:'course-eval-process',title:'评估样例：A100生产工艺',text:'本资料为课程虚构样例。A100鼠标经过点胶工序和组装工序。点胶工序使用点胶机DG01。点胶工序要求胶量检验。组装工序使用装配台AS01。A100鼠标要求外观检验。'},
 {key:'equipment',id:'course-eval-equipment',title:'评估样例：设备维护',text:'本资料为课程虚构样例。点胶机DG01由张师傅负责。点胶机DG01每班进行喷嘴点检。装配台AS01由李师傅负责。装配台AS01每周进行紧固检查。点胶设备DG01指代点胶机DG01。'},
 {key:'inspection',id:'course-eval-inspection',title:'评估样例：检验要求',text:'本资料为课程虚构样例。胶量检验使用电子秤SC01。胶量检验由王质检员执行。外观检验检查划痕和装配间隙。喷嘴点检要求清洁喷嘴。'}
]
export interface EvalCase {id:string;category:string;question:string;terms:string[][];files:string[];refuse?:boolean;version:1|2}
export const evaluationCases:EvalCase[]=[
 {id:'Q01',category:'单文件',question:'A100鼠标经过哪些工序？',terms:[['点胶'],['组装']],files:['process'],version:1},
 {id:'Q02',category:'单文件',question:'点胶工序使用什么设备？',terms:[['DG01']],files:['process'],version:1},
 {id:'Q03',category:'单文件',question:'组装工序使用什么设备？',terms:[['AS01']],files:['process'],version:1},
 {id:'Q04',category:'单文件',question:'点胶机DG01由谁负责？',terms:[['张师傅']],files:['equipment'],version:1},
 {id:'Q05',category:'单文件',question:'装配台AS01由谁负责？',terms:[['李师傅']],files:['equipment'],version:1},
 {id:'Q06',category:'单文件',question:'胶量检验使用什么仪器？',terms:[['SC01']],files:['inspection'],version:1},
 {id:'Q07',category:'单文件',question:'外观检验检查什么？',terms:[['划痕'],['间隙']],files:['inspection'],version:1},
 {id:'Q08',category:'跨文件',question:'点胶工序使用的设备由谁负责？',terms:[['DG01'],['张师傅']],files:['process','equipment'],version:1},
 {id:'Q09',category:'跨文件',question:'组装工序使用的设备由谁负责？',terms:[['AS01'],['李师傅']],files:['process','equipment'],version:1},
 {id:'Q10',category:'跨文件',question:'点胶工序要求的胶量检验使用什么仪器？',terms:[['胶量'],['SC01']],files:['process','inspection'],version:1},
 {id:'Q11',category:'三文件',question:'点胶工序使用什么设备、设备由谁负责、该工序要求的胶量检验由谁执行？',terms:[['DG01'],['张师傅'],['王质检员']],files:['process','equipment','inspection'],version:1},
 {id:'Q12',category:'同义表达',question:'DG01点胶设备的负责人是谁？',terms:[['张师傅']],files:['equipment'],version:1},
 {id:'Q13',category:'维护关联',question:'点胶机DG01做什么点检、该点检有什么要求？',terms:[['喷嘴'],['清洁']],files:['equipment','inspection'],version:1},
 {id:'Q14',category:'维护关联',question:'装配台AS01多久进行什么检查？',terms:[['每周'],['紧固']],files:['equipment'],version:1},
 {id:'Q15',category:'资料不足',question:'这些资料记载的2028年度净利润是多少？',terms:[],files:[],refuse:true,version:1},
 {id:'Q16',category:'资料不足',question:'资料中A100鼠标的销售单价是多少？',terms:[],files:[],refuse:true,version:1},
 {id:'Q17',category:'更新后',question:'当前版本点胶机DG01由谁负责？',terms:[['赵师傅']],files:['equipment'],version:2},
 {id:'Q18',category:'更新后跨文件',question:'当前版本点胶工序使用的设备由谁负责？',terms:[['DG01'],['赵师傅']],files:['process','equipment'],version:2}
]
