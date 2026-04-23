"use client"

import { useState, useEffect, use } from "react"
import { ArrowLeft, Edit, Download, CheckCircle2, Clock, GitBranch, Loader2, Archive, ChevronDown, ChevronUp } from "lucide-react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import * as z from "zod"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Badge } from "@/components/ui/badge"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { ProcessFlow } from "@/components/ui/process-flow"
import { Label } from "@/components/ui/label"
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible"
import type { Achievement, AchievementStatus } from "@/lib/types"

const getAuthToken = (): string | null => {
  if (typeof window !== 'undefined') {
    return localStorage.getItem('auth_token')
  }
  return null
}

const statusColors: Record<AchievementStatus, string> = {
  "预注册": "bg-yellow-100 text-yellow-800",
  "注册": "bg-blue-100 text-blue-800",
  "登记": "bg-green-100 text-green-800",
  "下架": "bg-gray-100 text-gray-600",
  "已删除": "bg-red-100 text-red-800",
}

const statusReverseMap: Record<string, string> = {
  "pre_register": "预注册",
  "registered": "注册",
  "recorded": "登记",
  "offline": "下架",
  "deleted": "已删除",
}

const checkoutFormSchema = z.object({
  purpose: z.string().min(1, "使用目的不能为空"),
})

const changeFormSchema = z.object({
  newVersion: z.string().min(1, "新版本号不能为空"),
  changeReason: z.string().min(1, "变更原因不能为空"),
  changeDescription: z.string().min(1, "变更描述不能为空"),
  changeType: z.string().min(1, "变更类型不能为空"),
})

interface AchievementDetail {
  id: string
  name: string
  version: string
  productVersion: string
  versionStatus: string
  versionType: string
  changeDescription: string
  delayReason: string
  acceptanceResult: string
  productId: string
  productName: string
  moduleId: string
  moduleName: string
  type: string
  description: string
  status: AchievementStatus
  productExternalVersion: string
  owner: string
  preRegisterTime: string
  registerTime: string | null
  recordTime: string | null
  relatedOrderId: string
  relatedProjectId: string
  relatedProjectName: string
  acceptanceRequirements: string
  attachments: any[]
  checkoutHistory: any[]
  versionHistory: any[]
  createdAt: string
  updatedAt: string
  organizationName: string
  departmentName: string
  hasBaseline: string
  requirementProposer: string
  achievementForm: string
  saleType: string
  applicationScenario: string
  achievementTarget: string
  plannedAcceptanceDate: string
  acceptanceMethod: string
  acceptor: string
  acceptanceOrganization: string
  changeReason: string
  deliverables: string
  codeRepositoryUrl: string
  demoUrl: string
  actualAcceptanceDate: string
  estimatedAcceptanceMonth: string
  createdBy: string
  updatedBy: string
}

export default function AchievementDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const router = useRouter()
  const { id } = use(params)
  const decodedId = decodeURIComponent(id)
  
  const [achievement, setAchievement] = useState<AchievementDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [activeTab, setActiveTab] = useState("details")
  const [activeProcessTab, setActiveProcessTab] = useState("meetings")
  const [selectedProcess, setSelectedProcess] = useState("achievement-management")
  const [checkoutDialogOpen, setCheckoutDialogOpen] = useState(false)
  const [changeDialogOpen, setChangeDialogOpen] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [loadingVersionHistory, setLoadingVersionHistory] = useState(false)
  const [basicDataOpen, setBasicDataOpen] = useState(true)
  const [planDataOpen, setPlanDataOpen] = useState(true)
  const [changeDataOpen, setChangeDataOpen] = useState(false)
  const [actualDataOpen, setActualDataOpen] = useState(false)
  const [systemDataOpen, setSystemDataOpen] = useState(false)

  const checkoutForm = useForm<z.infer<typeof checkoutFormSchema>>({
    resolver: zodResolver(checkoutFormSchema),
    defaultValues: {
      purpose: "",
    },
  })

  const changeForm = useForm<z.infer<typeof changeFormSchema>>({
    resolver: zodResolver(changeFormSchema),
    defaultValues: {
      newVersion: "",
      changeReason: "",
      changeDescription: "",
      changeType: "",
    },
  })

  const loadAchievement = async () => {
    try {
      setLoading(true)
      setError(null)
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8000'
      const response = await fetch(`${apiUrl}/api/achievements/${encodeURIComponent(decodedId)}`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
      })
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      const data = await response.json()
      setAchievement({
        id: data.id,
        name: data.name,
        version: data.version,
        productVersion: data.product_version || data.version,
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
        status: (statusReverseMap[data.status] || data.status) as AchievementStatus,
        productExternalVersion: data.product_external_version || '',
        owner: data.owner || '',
        preRegisterTime: data.pre_register_time ? new Date(data.pre_register_time).toISOString().split('T')[0] : '',
        registerTime: data.register_time ? new Date(data.register_time).toISOString().split('T')[0] : null,
        recordTime: data.record_time ? new Date(data.record_time).toISOString().split('T')[0] : null,
        relatedOrderId: data.related_order_id || '',
        relatedProjectId: data.related_project_id || '',
        relatedProjectName: data.related_project_name || '',
        acceptanceRequirements: data.acceptance_requirements || '',
        attachments: [],
        checkoutHistory: [],
        versionHistory: [],
        createdAt: new Date(data.created_at).toISOString().split('T')[0],
        updatedAt: new Date(data.updated_at).toISOString().split('T')[0],
        organizationName: data.organization_name || '',
        departmentName: data.department_name || '',
        hasBaseline: data.has_baseline || '无基线',
        requirementProposer: data.requirement_proposer || '',
        achievementForm: data.achievement_form || '',
        saleType: data.sale_type || '',
        applicationScenario: data.application_scenario || '',
        achievementTarget: data.achievement_target || '',
        plannedAcceptanceDate: data.planned_acceptance_date || '',
        acceptanceMethod: data.acceptance_method || '',
        acceptor: data.acceptor || '',
        acceptanceOrganization: data.acceptance_organization || '',
        changeReason: data.change_reason || '',
        deliverables: data.deliverables || '',
        codeRepositoryUrl: data.code_repository_url || '',
        demoUrl: data.demo_url || '',
        actualAcceptanceDate: data.actual_acceptance_date || '',
        estimatedAcceptanceMonth: data.estimated_acceptance_month || '',
        createdBy: data.created_by || '',
        updatedBy: data.updated_by || '',
      })
    } catch (err: any) {
      setError(err.message || '加载失败')
      console.error('加载成果详情失败:', err)
    } finally {
      setLoading(false)
    }
  }

  const loadVersionHistory = async () => {
    if (!achievement) return
    try {
      setLoadingVersionHistory(true)
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8000'
      const response = await fetch(`${apiUrl}/api/v1/achievement_version_records/${encodeURIComponent(decodedId)}`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
      })
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      const data = await response.json()
      setAchievement({
        ...achievement,
        versionHistory: data.items.map((item: any) => ({
          id: item.id,
          version: item.to_version,
          fromVersion: item.from_version,
          toVersion: item.to_version,
          changeDescription: item.change_description,
          changedFields: item.changed_fields,
          riskTags: item.risk_tags || [],
          operator: item.operator,
          changeTime: item.change_time ? new Date(item.change_time).toISOString().split('T')[0] : '',
        }))
      })
    } catch (err: any) {
      console.error('加载版本历史失败:', err)
    } finally {
      setLoadingVersionHistory(false)
    }
  }

  useEffect(() => {
    loadAchievement()
  }, [decodedId])

  useEffect(() => {
    if (activeTab === "version" && achievement && achievement.versionHistory.length === 0) {
      loadVersionHistory()
    }
  }, [activeTab, achievement])

  if (loading) {
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

  if (!achievement) {
    return (
      <div className="container mx-auto py-6">
        <Card>
          <CardContent className="py-12 text-center">
            <p className="text-muted-foreground mb-4">{error || '未找到该成果'}</p>
            <Button variant="outline" onClick={() => router.push("/achievement")}>
              返回列表
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  const handleCheckoutSubmit = async (values: z.infer<typeof checkoutFormSchema>) => {
    try {
      setSubmitting(true)
      const response = await fetch(`http://localhost:3040/api/achievements/${encodeURIComponent(decodedId)}/checkout`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          purpose: values.purpose,
          user_id: "current_user",
        }),
      })
      if (!response.ok) {
        const error = await response.json().catch(() => ({ detail: '出库失败' }))
        throw new Error(error.detail || '出库失败')
      }
      setCheckoutDialogOpen(false)
      checkoutForm.reset()
      alert('成果出库成功')
    } catch (err: any) {
      alert('出库失败: ' + (err.message || '未知错误'))
    } finally {
      setSubmitting(false)
    }
  }

  const handleChangeSubmit = async (values: z.infer<typeof changeFormSchema>) => {
    try {
      setSubmitting(true)
      const token = getAuthToken()
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
      }
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }
      const response = await fetch(`http://localhost:3040/api/achievements/${encodeURIComponent(decodedId)}/change`, {
        method: 'POST',
        headers,
        body: JSON.stringify({
          change_description: values.changeDescription,
        }),
      })
      if (!response.ok) {
        const error = await response.json().catch(() => ({ detail: '版本变更失败' }))
        throw new Error(error.detail || '版本变更失败')
      }
      setChangeDialogOpen(false)
      changeForm.reset()
      alert('版本变更成功')
      router.push('/achievement')
    } catch (err: any) {
      alert('版本变更失败: ' + (err.message || '未知错误'))
    } finally {
      setSubmitting(false)
    }
  }

  const canChange = achievement.status === "登记"

  const processPoints = [
    { id: "preRegister", name: "成果预注册" },
    { id: "register", name: "成果注册" },
    { id: "record", name: "成果登记" },
  ]

  const handlePointSelect = (pointId: string) => {
    switch (pointId) {
      case "preRegister":
        setActiveTab("preRegister")
        break
      case "register":
        setActiveTab("register")
        break
      case "record":
        setActiveTab("record")
        break
      default:
        break
    }
  }

  const renderField = (label: string, value: string | null | undefined) => {
    if (!value) return null
    return (
      <div>
        <Label className="text-[12px] font-medium text-muted-foreground">{label}</Label>
        <p className="text-[13px] mt-0.5">{value}</p>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div>
        <div className="flex items-center gap-2 mb-1">
          <Button variant="ghost" size="icon" asChild className="h-8 w-8">
            <Link href="/achievement">
              <ArrowLeft className="h-4 w-4" />
              <span className="sr-only">返回</span>
            </Link>
          </Button>
          <h1 className="text-lg font-semibold">成果</h1>
        </div>
        <p className="text-muted-foreground ml-10 text-[13px]">
          {achievement.name}
        </p>
      </div>

      <div className="flex justify-between items-center">
        <div className="w-[200px]">
          <Select value={selectedProcess} onValueChange={setSelectedProcess}>
            <SelectTrigger className="h-8 text-[13px]">
              <SelectValue placeholder="成果管理流程" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="achievement-management">成果管理流程</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div className="flex items-center space-x-2">
          <Badge className={`${statusColors[achievement.status]} text-[12px]`}>
            {achievement.status}
          </Badge>
          <Button variant="outline" size="sm" asChild className="h-7 text-[12px]">
            <Link href={`/achievement/change?achievement_id=${encodeURIComponent(decodedId)}`}>
              <Edit className="h-3 w-3 mr-1" />
              变更
            </Link>
          </Button>
          {canChange && (
            <Dialog open={changeDialogOpen} onOpenChange={setChangeDialogOpen}>
              <DialogTrigger asChild>
                <Button variant="outline" size="sm" className="h-7 text-[12px]">
                  <GitBranch className="h-3 w-3 mr-1" />
                  变更版本
                </Button>
              </DialogTrigger>
              <DialogContent className="max-w-md">
                <DialogHeader>
                  <DialogTitle>变更成果版本</DialogTitle>
                  <DialogDescription>
                    变更后将生成新的成果ID，当前版本将被锁定
                  </DialogDescription>
                </DialogHeader>
                <Form {...changeForm}>
                  <form onSubmit={changeForm.handleSubmit(handleChangeSubmit)} className="space-y-4">
                    <FormField
                      control={changeForm.control}
                      name="newVersion"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>新版本号</FormLabel>
                          <FormControl>
                            <Input placeholder="例如：V1.1.0" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={changeForm.control}
                      name="changeType"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>变更类型</FormLabel>
                          <Select onValueChange={field.onChange} value={field.value}>
                            <FormControl>
                              <SelectTrigger>
                                <SelectValue placeholder="选择变更类型" />
                              </SelectTrigger>
                            </FormControl>
                            <SelectContent>
                              <SelectItem value="新增">新增</SelectItem>
                              <SelectItem value="修改">修改</SelectItem>
                              <SelectItem value="删除">删除</SelectItem>
                            </SelectContent>
                          </Select>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={changeForm.control}
                      name="changeReason"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>变更原因</FormLabel>
                          <FormControl>
                            <Textarea placeholder="请说明变更原因" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={changeForm.control}
                      name="changeDescription"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>变更描述</FormLabel>
                          <FormControl>
                            <Textarea placeholder="详细描述变更内容" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    <DialogFooter>
                      <Button type="button" variant="outline" onClick={() => setChangeDialogOpen(false)} disabled={submitting}>
                        取消
                      </Button>
                      <Button type="submit" disabled={submitting}>
                        {submitting && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                        确认变更
                      </Button>
                    </DialogFooter>
                  </form>
                </Form>
              </DialogContent>
            </Dialog>
          )}
          <Dialog open={checkoutDialogOpen} onOpenChange={setCheckoutDialogOpen}>
            <DialogTrigger asChild>
              <Button variant="outline" size="sm" className="h-7 text-[12px]">
                <Download className="h-3 w-3 mr-1" />
                成果出库
              </Button>
            </DialogTrigger>
            <DialogContent className="max-w-md">
              <DialogHeader>
                <DialogTitle>成果出库</DialogTitle>
                <DialogDescription>
                  记录成果使用和下载信息
                </DialogDescription>
              </DialogHeader>
              <Form {...checkoutForm}>
                <form onSubmit={checkoutForm.handleSubmit(handleCheckoutSubmit)} className="space-y-4">
                  <FormField
                    control={checkoutForm.control}
                    name="purpose"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>使用目的</FormLabel>
                        <FormControl>
                          <Textarea placeholder="请说明使用目的" {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <DialogFooter>
                    <Button type="button" variant="outline" onClick={() => setCheckoutDialogOpen(false)} disabled={submitting}>
                      取消
                    </Button>
                    <Button type="submit" disabled={submitting}>
                      {submitting && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                      确认出库
                    </Button>
                  </DialogFooter>
                </form>
              </Form>
            </DialogContent>
          </Dialog>
        </div>
      </div>

      <Card>
        <CardContent className="pt-4">
          <ProcessFlow
            points={processPoints}
            selectedPoint={
              activeTab === "details"
                ? undefined
                : activeTab === "attachments"
                  ? undefined
                  : activeTab === "checkout"
                    ? undefined
                    : activeTab === "version"
                      ? undefined
                      : activeTab
            }
            onSelectPoint={handlePointSelect}
          />
        </CardContent>
      </Card>

      <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
        <TabsList className="flex overflow-x-auto pb-px mb-3 justify-start h-8">
          <TabsTrigger value="details" className="flex-shrink-0 text-[12px] h-7">
            成果详情
          </TabsTrigger>
          <TabsTrigger value="preRegister" className="flex-shrink-0 text-[12px] h-7">
            成果预注册
          </TabsTrigger>
          <TabsTrigger value="register" className="flex-shrink-0 text-[12px] h-7">
            成果注册
          </TabsTrigger>
          <TabsTrigger value="record" className="flex-shrink-0 text-[12px] h-7">
            成果登记
          </TabsTrigger>
          <TabsTrigger value="attachments" className="flex-shrink-0 text-[12px] h-7">
            附件列表
          </TabsTrigger>
          <TabsTrigger value="checkout" className="flex-shrink-0 text-[12px] h-7">
            出库历史
          </TabsTrigger>
          <TabsTrigger value="version" className="flex-shrink-0 text-[12px] h-7">
            版本历史
          </TabsTrigger>
        </TabsList>

        <TabsContent value="details" className="mt-2 space-y-3">
          <Collapsible open={basicDataOpen} onOpenChange={setBasicDataOpen}>
            <CollapsibleTrigger className="flex items-center justify-between w-full p-2 bg-slate-50 rounded-lg hover:bg-slate-100 transition-colors">
              <span className="font-semibold text-[14px]">基础数据</span>
              {basicDataOpen ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
            </CollapsibleTrigger>
            <CollapsibleContent className="pt-3">
              <div className="grid grid-cols-1 md:grid-cols-4 gap-x-6 gap-y-3">
                {renderField("成果ID", achievement.id)}
                {renderField("成果名称", achievement.name)}
                {renderField("成果版本", achievement.version)}
                {renderField("所属产品", achievement.productName)}
                {renderField("所属机构", achievement.organizationName)}
                {renderField("所属部门", achievement.departmentName)}
                {renderField("是否有基线", achievement.hasBaseline)}
                {renderField("成果需求提出人", achievement.requirementProposer)}
                {renderField("成果形态", achievement.achievementForm)}
                {renderField("成果可售类型", achievement.saleType)}
                {renderField("所属模块", achievement.moduleName)}
                {renderField("成果类型", achievement.type)}
                {renderField("负责人", achievement.owner)}
              </div>
              <div className="mt-3 space-y-3">
                {renderField("成果描述", achievement.description)}
                {renderField("应用场景", achievement.applicationScenario)}
              </div>
            </CollapsibleContent>
          </Collapsible>

          <Collapsible open={planDataOpen} onOpenChange={setPlanDataOpen}>
            <CollapsibleTrigger className="flex items-center justify-between w-full p-2 bg-slate-50 rounded-lg hover:bg-slate-100 transition-colors">
              <span className="font-semibold text-[14px]">计划数据</span>
              {planDataOpen ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
            </CollapsibleTrigger>
            <CollapsibleContent className="pt-3">
              <div className="grid grid-cols-1 md:grid-cols-4 gap-x-6 gap-y-3">
                {renderField("计划验收日期", achievement.plannedAcceptanceDate)}
                {renderField("成果验收人", achievement.acceptor)}
                {renderField("验收机构", achievement.acceptanceOrganization)}
                {renderField("关联项目", achievement.relatedProjectName)}
                {renderField("关联订单", achievement.relatedOrderId)}
              </div>
              <div className="mt-3 space-y-3">
                {renderField("成果目标", achievement.achievementTarget)}
                {renderField("验收方式", achievement.acceptanceMethod)}
                {renderField("验收要求", achievement.acceptanceRequirements)}
              </div>
            </CollapsibleContent>
          </Collapsible>

          <Collapsible open={changeDataOpen} onOpenChange={setChangeDataOpen}>
            <CollapsibleTrigger className="flex items-center justify-between w-full p-2 bg-slate-50 rounded-lg hover:bg-slate-100 transition-colors">
              <span className="font-semibold text-[14px]">变更数据</span>
              {changeDataOpen ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
            </CollapsibleTrigger>
            <CollapsibleContent className="pt-3">
              <div className="space-y-3">
                {renderField("变更原因", achievement.changeReason)}
                {renderField("变更说明", achievement.changeDescription)}
              </div>
            </CollapsibleContent>
          </Collapsible>

          <Collapsible open={actualDataOpen} onOpenChange={setActualDataOpen}>
            <CollapsibleTrigger className="flex items-center justify-between w-full p-2 bg-slate-50 rounded-lg hover:bg-slate-100 transition-colors">
              <span className="font-semibold text-[14px]">实际数据</span>
              {actualDataOpen ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
            </CollapsibleTrigger>
            <CollapsibleContent className="pt-3">
              <div className="grid grid-cols-1 md:grid-cols-4 gap-x-6 gap-y-3">
                {renderField("实际验收日期", achievement.actualAcceptanceDate)}
                {renderField("代码仓库地址", achievement.codeRepositoryUrl)}
                {renderField("DEMO地址", achievement.demoUrl)}
              </div>
              <div className="mt-3 space-y-3">
                {renderField("成果提交物", achievement.deliverables)}
                {renderField("验收结果", achievement.acceptanceResult)}
              </div>
            </CollapsibleContent>
          </Collapsible>

          <Collapsible open={systemDataOpen} onOpenChange={setSystemDataOpen}>
            <CollapsibleTrigger className="flex items-center justify-between w-full p-2 bg-slate-50 rounded-lg hover:bg-slate-100 transition-colors">
              <span className="font-semibold text-[14px]">系统审计数据</span>
              {systemDataOpen ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
            </CollapsibleTrigger>
            <CollapsibleContent className="pt-3">
              <div className="grid grid-cols-1 md:grid-cols-4 gap-x-6 gap-y-3">
                {renderField("状态", achievement.status)}
                {renderField("预估验收年月", achievement.estimatedAcceptanceMonth)}
                {renderField("预注册时间", achievement.preRegisterTime)}
                {renderField("注册时间", achievement.registerTime)}
                {renderField("登记时间", achievement.recordTime)}
                {renderField("创建人", achievement.createdBy)}
                {renderField("更新人", achievement.updatedBy)}
                {renderField("创建时间", achievement.createdAt)}
                {renderField("更新时间", achievement.updatedAt)}
              </div>
            </CollapsibleContent>
          </Collapsible>
        </TabsContent>

        <TabsContent value="preRegister" className="mt-4">
          <Card>
            <CardHeader className="py-3">
              <CardTitle className="text-[16px]">成果预注册</CardTitle>
              <CardDescription className="text-[12px]">成果预注册阶段信息</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                {renderField("预注册时间", achievement.preRegisterTime)}
                {renderField("负责人", achievement.owner)}
                {renderField("成果描述", achievement.description)}
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="register" className="mt-4">
          <Card>
            <CardHeader className="py-3">
              <CardTitle className="text-[16px]">成果注册</CardTitle>
              <CardDescription className="text-[12px]">成果注册阶段信息</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                {renderField("注册时间", achievement.registerTime || "未注册")}
                {renderField("验收要求", achievement.acceptanceRequirements)}
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="record" className="mt-4">
          <Card>
            <CardHeader className="py-3">
              <CardTitle className="text-[16px]">成果登记</CardTitle>
              <CardDescription className="text-[12px]">成果登记阶段信息</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                {renderField("登记时间", achievement.recordTime || "未登记")}
                {renderField("关联订单", achievement.relatedOrderId)}
                {renderField("关联项目", achievement.relatedProjectName)}
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="attachments" className="mt-4">
          <Card>
            <CardHeader className="py-3">
              <CardTitle className="text-[16px]">附件列表</CardTitle>
              <CardDescription className="text-[12px]">成果相关附件</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="text-center py-8 text-muted-foreground text-[13px]">
                暂无附件
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="checkout" className="mt-4">
          <Card>
            <CardHeader className="py-3">
              <CardTitle className="text-[16px]">出库历史</CardTitle>
              <CardDescription className="text-[12px]">成果出库记录</CardDescription>
            </CardHeader>
            <CardContent>
              {achievement.checkoutHistory.length > 0 ? (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="text-[12px]">出库时间</TableHead>
                      <TableHead className="text-[12px]">使用人</TableHead>
                      <TableHead className="text-[12px]">使用目的</TableHead>
                      <TableHead className="text-[12px]">下载次数</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {achievement.checkoutHistory.map((history) => (
                      <TableRow key={history.id}>
                        <TableCell className="text-[12px]">{history.checkoutTime}</TableCell>
                        <TableCell className="text-[12px]">{history.userName}</TableCell>
                        <TableCell className="text-[12px]">{history.purpose}</TableCell>
                        <TableCell className="text-[12px]">{history.downloadCount}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              ) : (
                <div className="text-center py-8 text-muted-foreground text-[13px]">
                  暂无出库记录
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="version" className="mt-4">
          <Card>
            <CardHeader className="py-3">
              <CardTitle className="text-[16px]">版本历史</CardTitle>
              <CardDescription className="text-[12px]">成果版本变更记录</CardDescription>
            </CardHeader>
            <CardContent>
              {loadingVersionHistory ? (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="h-6 w-6 animate-spin mr-2" />
                  <span className="text-[13px] text-muted-foreground">加载版本历史...</span>
                </div>
              ) : achievement.versionHistory.length > 0 ? (
                <div className="relative">
                  <div className="absolute left-4 top-0 bottom-0 w-0.5 bg-gray-200"></div>
                  <div className="space-y-4">
                    {achievement.versionHistory.map((history) => (
                      <div key={history.id} className="relative pl-10">
                        <div className="absolute left-2.5 w-3 h-3 bg-blue-500 rounded-full border-2 border-white"></div>
                        <div className="bg-gray-50 rounded-lg p-3 border">
                          <div className="flex items-center justify-between mb-2">
                            <h4 className="font-semibold text-[13px]">{history.toVersion}</h4>
                            <div className="text-[11px] text-gray-500">
                              {history.fromVersion && <span className="mr-2">{history.fromVersion} → </span>}
                              {history.toVersion}
                            </div>
                          </div>
                          <p className="text-[12px] text-gray-600 mb-2">{history.changeDescription}</p>
                          <div className="text-[11px] text-gray-500 space-y-1">
                            <p><span className="font-medium">操作人：</span>{history.operator}</p>
                            <p><span className="font-medium">操作时间：</span>{history.changeTime}</p>
                            {history.changedFields && Object.keys(history.changedFields).length > 0 && (
                              <div className="mt-2 pt-2 border-t">
                                <p className="font-medium mb-1">变更字段：</p>
                                {Object.entries(history.changedFields).map(([field, values]: [string, any]) => (
                                  <p key={field} className="text-gray-600">
                                    • {field}: {values.old} → {values.new}
                                  </p>
                                ))}
                              </div>
                            )}
                            {history.riskTags && history.riskTags.length > 0 && (
                              <div className="flex items-center gap-1 flex-wrap mt-2">
                                <span className="font-medium">风险标签：</span>
                                {history.riskTags.map((tag: string, idx: number) => (
                                  <Badge key={idx} variant="secondary" className="text-[11px]">
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
                <div className="text-center py-8 text-muted-foreground text-[13px]">
                  暂无版本历史
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  )
}
