"use client"

import { useState, useEffect } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import * as z from "zod"
import { Badge } from "@/components/ui/badge"
import { Label } from "@/components/ui/label"
import { ArrowLeft, Clock, User, Calendar, CheckCircle2, Loader2, AlertCircle, FileText, Package, Projector, ClipboardList, Archive } from "lucide-react"
import type { Achievement } from "@/lib/types"

const getAuthToken = (): string | null => {
  if (typeof window !== 'undefined') {
    return localStorage.getItem('auth_token')
  }
  return null
}

const statusColors: Record<string, string> = {
  "预注册": "bg-yellow-100 text-yellow-800",
  "注册": "bg-blue-100 text-blue-800",
  "登记": "bg-green-100 text-green-800",
  "下架": "bg-gray-100 text-gray-600",
}

const statusIcons: Record<string, React.ReactNode> = {
  "预注册": <Clock className="h-3 w-3" />,
  "注册": <CheckCircle2 className="h-3 w-3" />,
  "登记": <CheckCircle2 className="h-3 w-3" />,
  "下架": <Archive className="h-3 w-3" />,
}

const statusReverseMap: Record<string, string> = {
  "pre_register": "预注册",
  "register": "注册",
  "record": "登记",
  "offline": "下架",
}

const changeFormSchema = z.object({
  changeDescription: z.string().min(1, "变更说明不能为空"),
  description: z.string().optional(),
  owner: z.string().optional(),
  relatedProjectId: z.string().optional(),
  relatedOrderId: z.string().optional(),
  acceptanceRequirements: z.string().optional(),
  plannedCompletionTime: z.string().optional(),
  productExternalVersion: z.string().optional(),
  moduleName: z.string().optional(),
})

export default function AchievementChangePage() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const achievementId = searchParams.get("achievement_id")
  
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [achievement, setAchievement] = useState<Achievement | null>(null)
  const [versionRecords, setVersionRecords] = useState<any[]>([])
  const [loadingAchievement, setLoadingAchievement] = useState(false)
  const [loadingVersionRecords, setLoadingVersionRecords] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const form = useForm<z.infer<typeof changeFormSchema>>({
    resolver: zodResolver(changeFormSchema),
    defaultValues: {
      changeDescription: "",
      description: "",
      owner: "",
      relatedProjectId: "",
      relatedOrderId: "",
      acceptanceRequirements: "",
      plannedCompletionTime: "",
      productExternalVersion: "",
      moduleName: "",
    },
  })

  const loadAchievement = async (id: string) => {
    try {
      setLoadingAchievement(true)
      setError(null)
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8000'
      const response = await fetch(`${apiUrl}/api/achievements/${encodeURIComponent(id)}`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
      })
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      const data = await response.json()
      const achievementData: Achievement = {
        id: data.id,
        name: data.name,
        version: data.version,
        productVersion: data.product_version || data.version,
        productExternalVersion: data.product_external_version || '',
        versionStatus: data.version_status || '正常',
        versionType: data.version_type || '当期创建',
        changeDescription: data.change_description || '',
        delayReason: data.delay_reason || '',
        acceptanceResult: data.acceptance_result || '',
        productId: data.product_id,
        productName: data.product_name || '',
        moduleId: data.module_id || '',
        moduleName: data.module_name || '',
        type: data.type || '其他',
        description: data.description || '',
        status: statusReverseMap[data.status] || data.status,
        owner: data.owner || '',
        preRegisterTime: data.pre_register_time ? new Date(data.pre_register_time).toISOString().split('T')[0] : '',
        registerTime: data.register_time ? new Date(data.register_time).toISOString().split('T')[0] : null,
        recordTime: data.record_time ? new Date(data.record_time).toISOString().split('T')[0] : null,
        relatedOrderId: data.related_order_id || '',
        relatedProjectId: data.related_project_id || '',
        acceptanceRequirements: data.acceptance_requirements || '',
        attachments: [],
        checkoutHistory: [],
        versionHistory: [],
        createdAt: new Date(data.created_at).toISOString().split('T')[0],
        updatedAt: new Date(data.updated_at).toISOString().split('T')[0],
      }
      setAchievement(achievementData)
      form.setValue("description", achievementData.description || "")
      form.setValue("owner", achievementData.owner || "")
      form.setValue("relatedProjectId", achievementData.relatedProjectId || "")
      form.setValue("relatedOrderId", achievementData.relatedOrderId || "")
      form.setValue("acceptanceRequirements", achievementData.acceptanceRequirements || "")
      form.setValue("plannedCompletionTime", achievementData.registerTime || "")
      form.setValue("productExternalVersion", achievementData.productExternalVersion || "")
      form.setValue("moduleName", achievementData.moduleName || "")
    } catch (err: any) {
      setError(err.message || '加载成果失败')
      console.error('加载成果失败:', err)
    } finally {
      setLoadingAchievement(false)
    }
  }

  const loadVersionRecords = async (id: string) => {
    try {
      setLoadingVersionRecords(true)
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8000'
      const url = `${apiUrl}/api/v1/achievement_version_records/${encodeURIComponent(id)}`
      const response = await fetch(url, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
      })
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      const data = await response.json()
      setVersionRecords(data.items)
    } catch (err: any) {
      console.error('加载变更记录失败:', err)
    } finally {
      setLoadingVersionRecords(false)
    }
  }

  useEffect(() => {
    if (achievementId) {
      loadAchievement(achievementId)
      loadVersionRecords(achievementId)
    }
  }, [achievementId])

  const handleSubmit = async (values: z.infer<typeof changeFormSchema>) => {
    if (!achievement) {
      alert("成果信息加载失败")
      return
    }

    setIsSubmitting(true)
    try {
      const requestBody: any = {
        change_description: values.changeDescription,
      }
      
      if (values.description && values.description !== achievement.description) {
        requestBody.description = values.description
      }
      if (values.owner && values.owner !== achievement.owner) {
        requestBody.owner = values.owner
      }
      if (values.relatedProjectId && values.relatedProjectId !== achievement.relatedProjectId) {
        requestBody.related_project_id = values.relatedProjectId
      }
      if (values.relatedOrderId && values.relatedOrderId !== achievement.relatedOrderId) {
        requestBody.related_order_id = values.relatedOrderId
      }
      if (values.acceptanceRequirements && values.acceptanceRequirements !== achievement.acceptanceRequirements) {
        requestBody.acceptance_requirements = values.acceptanceRequirements
      }
      if (values.plannedCompletionTime) {
        requestBody.register_time = values.plannedCompletionTime
      }
      if (values.productExternalVersion && values.productExternalVersion !== achievement.productExternalVersion) {
        requestBody.product_external_version = values.productExternalVersion
      }
      if (values.moduleName && values.moduleName !== achievement.moduleName) {
        requestBody.module_name = values.moduleName
      }

      const token = getAuthToken()
      if (!token) {
        alert('请先登录系统')
        router.push('/login')
        return
      }

      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      }

      const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8000'
      const response = await fetch(`${apiUrl}/api/achievements/${encodeURIComponent(achievement.id)}/change`, {
        method: 'POST',
        headers,
        body: JSON.stringify(requestBody),
      })

      if (!response.ok) {
        const error = await response.json().catch(() => ({ detail: '变更提交失败' }))
        if (response.status === 401) {
          alert('登录已过期，请重新登录')
          router.push('/login')
          return
        }
        throw new Error(error.detail || '变更提交失败')
      }

      alert('变更提交成功！成果版本已更新')
      router.push('/achievement')
    } catch (error: any) {
      console.error("变更失败:", error)
      alert('变更失败: ' + (error.message || '未知错误'))
    } finally {
      setIsSubmitting(false)
    }
  }

  const getChangedFieldsDisplay = (record: any) => {
    const changes: string[] = []
    
    const fieldNameMap: Record<string, string> = {
      'description': '成果描述',
      'owner': '负责人',
      'related_project_id': '关联项目',
      'related_order_id': '关联订单',
      'acceptance_requirements': '验收要求',
      'register_time': '计划完工时间',
      'product_external_version': '成果对外版本',
      'module_id': '所属模块ID',
      'module_name': '所属模块',
    }
    
    if (record.changed_fields && typeof record.changed_fields === 'object') {
      Object.entries(record.changed_fields).forEach(([field, values]: [string, any]) => {
        if (values && values.old !== undefined && values.new !== undefined) {
          const fieldName = fieldNameMap[field] || field
          changes.push(`${fieldName}: ${values.old || '空'} → ${values.new || '空'}`)
        }
      })
    }
    
    return changes
  }

  if (!achievementId) {
    return (
      <div className="container mx-auto py-6">
        <Card>
          <CardContent className="py-12 text-center">
            <AlertCircle className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
            <p className="text-muted-foreground mb-4">缺少成果ID参数</p>
            <Button variant="outline" onClick={() => router.push("/achievement")}>
              返回列表
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  if (loadingAchievement) {
    return (
      <div className="container mx-auto py-6">
        <Card>
          <CardContent className="py-12 text-center">
            <Loader2 className="h-8 w-8 animate-spin mx-auto mb-4" />
            <p className="text-muted-foreground">加载中...</p>
          </CardContent>
        </Card>
      </div>
    )
  }

  if (error || !achievement) {
    return (
      <div className="container mx-auto py-6">
        <Card>
          <CardContent className="py-12 text-center">
            <AlertCircle className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
            <p className="text-muted-foreground mb-4">{error || '未找到该成果'}</p>
            <Button variant="outline" onClick={() => router.push("/achievement")}>
              返回列表
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="container mx-auto py-6 space-y-6">
      <div className="flex items-center gap-2 mb-1">
        <Button variant="ghost" size="icon" asChild className="h-8 w-8">
          <a href="/achievement">
            <ArrowLeft className="h-4 w-4" />
            <span className="sr-only">返回</span>
          </a>
        </Button>
        <h1 className="text-xl font-semibold">成果变更</h1>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div>
          <Card>
            <CardHeader>
              <CardTitle className="text-[18px]">变更表单</CardTitle>
              <CardDescription>填写变更信息</CardDescription>
            </CardHeader>
            <CardContent>
              <Form {...form}>
                <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-6">
                  <div className="bg-gray-50 p-4 rounded-lg space-y-3">
                    <h4 className="font-medium text-sm">基础信息（只读）</h4>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
                      <div>
                        <Label className="text-xs text-muted-foreground">成果ID</Label>
                        <p className="font-medium">{achievement.id}</p>
                      </div>
                      <div>
                        <Label className="text-xs text-muted-foreground">成果名称</Label>
                        <p className="font-medium">{achievement.name}</p>
                      </div>
                      <div>
                        <Label className="text-xs text-muted-foreground">成果类型</Label>
                        <p className="font-medium">{achievement.type}</p>
                      </div>
                      <div>
                        <Label className="text-xs text-muted-foreground">所属产品</Label>
                        <p className="font-medium">{achievement.productName}</p>
                      </div>
                      <div>
                        <Label className="text-xs text-muted-foreground">所属模块</Label>
                        <p className="font-medium">{achievement.moduleName}</p>
                      </div>
                      <div>
                        <Label className="text-xs text-muted-foreground">当前版本</Label>
                        <p className="font-medium">{achievement.version}</p>
                      </div>
                    </div>
                  </div>

                  <div className="space-y-4">
                    <h4 className="font-medium text-sm">可编辑字段</h4>
                    
                    <FormField
                      control={form.control}
                      name="description"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="flex items-center gap-2">
                            <FileText className="h-4 w-4" />
                            成果描述
                          </FormLabel>
                          <FormControl>
                            <Textarea 
                              placeholder="请输入成果描述" 
                              className="min-h-[100px]" 
                              {...field} 
                            />
                          </FormControl>
                          <FormDescription>
                            当前值：{achievement.description || '无'}
                          </FormDescription>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={form.control}
                      name="owner"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="flex items-center gap-2">
                            <User className="h-4 w-4" />
                            负责人
                          </FormLabel>
                          <FormControl>
                            <Input placeholder="请输入负责人" {...field} />
                          </FormControl>
                          <FormDescription>
                            当前值：{achievement.owner || '无'}
                          </FormDescription>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={form.control}
                      name="productExternalVersion"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="flex items-center gap-2">
                            <Package className="h-4 w-4" />
                            成果对外版本
                          </FormLabel>
                          <FormControl>
                            <Input placeholder="请输入成果对外版本" {...field} />
                          </FormControl>
                          <FormDescription>
                            当前值：{achievement.productExternalVersion || '无'}
                          </FormDescription>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={form.control}
                      name="moduleName"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="flex items-center gap-2">
                            <FileText className="h-4 w-4" />
                            所属模块
                          </FormLabel>
                          <FormControl>
                            <Input placeholder="请输入所属模块名称" {...field} />
                          </FormControl>
                          <FormDescription>
                            当前值：{achievement.moduleName || '无'}
                          </FormDescription>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={form.control}
                      name="relatedProjectId"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="flex items-center gap-2">
                            <Projector className="h-4 w-4" />
                            关联项目
                          </FormLabel>
                          <FormControl>
                            <Input placeholder="请输入关联项目ID" {...field} />
                          </FormControl>
                          <FormDescription>
                            当前值：{achievement.relatedProjectId || '无'}
                          </FormDescription>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={form.control}
                      name="relatedOrderId"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="flex items-center gap-2">
                            <Package className="h-4 w-4" />
                            关联订单
                          </FormLabel>
                          <FormControl>
                            <Input placeholder="请输入关联订单ID" {...field} />
                          </FormControl>
                          <FormDescription>
                            当前值：{achievement.relatedOrderId || '无'}
                          </FormDescription>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={form.control}
                      name="acceptanceRequirements"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="flex items-center gap-2">
                            <ClipboardList className="h-4 w-4" />
                            验收要求
                          </FormLabel>
                          <FormControl>
                            <Textarea 
                              placeholder="请输入验收要求" 
                              className="min-h-[80px]" 
                              {...field} 
                            />
                          </FormControl>
                          <FormDescription>
                            当前值：{achievement.acceptanceRequirements || '无'}
                          </FormDescription>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={form.control}
                      name="plannedCompletionTime"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="flex items-center gap-2">
                            <Calendar className="h-4 w-4" />
                            计划完工时间
                          </FormLabel>
                          <FormControl>
                            <Input type="date" {...field} />
                          </FormControl>
                          <FormDescription>
                            当前值：{achievement.registerTime || '无'}
                          </FormDescription>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>

                  <FormField
                    control={form.control}
                    name="changeDescription"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>变更说明 <span className="text-red-500">*</span></FormLabel>
                        <FormControl>
                          <Textarea 
                            placeholder="请详细说明变更的原因和内容" 
                            className="min-h-[120px]" 
                            {...field} 
                          />
                        </FormControl>
                        <FormDescription>详细说明变更的原因和内容</FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <div className="flex justify-end space-x-4 pt-4">
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => router.push("/achievement")}
                      disabled={isSubmitting}
                    >
                      取消
                    </Button>
                    <Button type="submit" disabled={isSubmitting}>
                      {isSubmitting && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                      {isSubmitting ? "提交中..." : "提交变更"}
                    </Button>
                  </div>
                </form>
              </Form>
            </CardContent>
          </Card>
        </div>

        <div>
          <Card>
            <CardHeader>
              <CardTitle className="text-[18px]">变更历史</CardTitle>
              <CardDescription>成果变更记录</CardDescription>
            </CardHeader>
            <CardContent>
              {loadingVersionRecords ? (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="h-6 w-6 animate-spin mr-2" />
                  <span className="text-sm text-muted-foreground">加载变更记录...</span>
                </div>
              ) : versionRecords.length > 0 ? (
                <div className="relative">
                  <div className="absolute left-4 top-0 bottom-0 w-0.5 bg-gray-200"></div>
                  <div className="space-y-6">
                    {versionRecords.map((record, index) => (
                      <div key={record.id} className="relative pl-10">
                        <div className="absolute left-2.5 w-3 h-3 bg-blue-500 rounded-full border-2 border-white"></div>
                        <div className="bg-gray-50 rounded-lg p-4 border">
                          <div className="flex items-center justify-between mb-2">
                            <h4 className="font-semibold text-sm">{record.to_version}</h4>
                            <div className="text-xs text-gray-500">
                              {record.from_version && <span className="mr-2">{record.from_version} → </span>}
                              {record.to_version}
                            </div>
                          </div>
                          <p className="text-sm text-gray-600 mb-2">{record.change_description}</p>
                          <div className="text-xs text-gray-500 space-y-1">
                            <p><span className="font-medium">操作人：</span>{record.operator}</p>
                            <p><span className="font-medium">操作时间：</span>{new Date(record.change_time).toLocaleString('zh-CN')}</p>
                            {getChangedFieldsDisplay(record).length > 0 && (
                              <div className="mt-2 pt-2 border-t">
                                <p className="font-medium mb-1">变更字段：</p>
                                {getChangedFieldsDisplay(record).map((change, idx) => (
                                  <p key={idx} className="text-gray-600">• {change}</p>
                                ))}
                              </div>
                            )}
                            {record.risk_tags && record.risk_tags.length > 0 && (
                              <div className="flex items-center gap-1 flex-wrap mt-2">
                                <span className="font-medium">风险标签：</span>
                                {record.risk_tags.map((tag: string, idx: number) => (
                                  <Badge key={idx} variant="secondary" className="text-xs">
                                    {tag}
                                  </Badge>
                                ))}
                              </div>
                            )}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : (
                <div className="text-center py-8 text-muted-foreground">
                  暂无变更记录
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}
