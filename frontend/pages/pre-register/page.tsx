"use client"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import * as z from "zod"
import { useRouter } from "next/navigation"
import { useState, useEffect } from "react"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Upload, X, Loader2, ChevronDown, ChevronUp } from "lucide-react"
import type { Product } from "@/lib/types"
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible"

const formSchema = z.object({
  name: z.string().min(1, "成果名称不能为空"),
  productId: z.string().min(1, "所属产品不能为空"),
  organizationName: z.string().min(1, "所属机构不能为空"),
  departmentName: z.string().min(1, "所属部门不能为空"),
  version: z.string().min(1, "成果版本不能为空"),
  hasBaseline: z.string().min(1, "是否有基线不能为空"),
  requirementProposer: z.string().min(1, "成果需求提出人不能为空"),
  achievementForm: z.string().min(1, "成果形态不能为空"),
  saleType: z.string().min(1, "成果可售类型不能为空"),
  applicationScenario: z.string().min(1, "应用场景不能为空"),
  type: z.string().min(1, "成果类型不能为空"),
  description: z.string().min(1, "成果描述不能为空"),
  owner: z.string().min(1, "负责人不能为空"),
  achievementTarget: z.string().min(1, "成果目标不能为空"),
  plannedAcceptanceDate: z.string().min(1, "计划验收日期不能为空"),
  acceptanceMethod: z.string().min(1, "验收方式不能为空"),
  acceptor: z.string().min(1, "成果验收人不能为空"),
  relatedProjectName: z.string().optional(),
  relatedOrderId: z.string().optional(),
  relatedOrderName: z.string().optional(),
  acceptanceRequirements: z.string().min(1, "验收要求不能为空"),
})

interface UploadedFile {
  id: string
  name: string
  size: number
  type: string
}

export default function AchievementPreRegisterPage() {
  const router = useRouter()
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [uploadedFiles, setUploadedFiles] = useState<UploadedFile[]>([])
  const [products, setProducts] = useState<Product[]>([])
  const [loadingProducts, setLoadingProducts] = useState(false)
  const [basicDataOpen, setBasicDataOpen] = useState(true)
  const [planDataOpen, setPlanDataOpen] = useState(true)
  const [execPlanDataOpen, setExecPlanDataOpen] = useState(false)

  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      name: "",
      productId: "",
      organizationName: "",
      departmentName: "",
      version: "",
      hasBaseline: "无基线",
      requirementProposer: "",
      achievementForm: "",
      saleType: "",
      applicationScenario: "",
      type: "",
      description: "",
      owner: "",
      achievementTarget: "",
      plannedAcceptanceDate: "",
      acceptanceMethod: "",
      acceptor: "",
      relatedProjectName: "",
      relatedOrderId: "",
      relatedOrderName: "",
      acceptanceRequirements: "",
    },
  })

  const loadProducts = async () => {
    try {
      setLoadingProducts(true)
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8000'
      const response = await fetch(`${apiUrl}/api/products?page_size=100`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
      })
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      const data = await response.json()
      setProducts(data.items)
    } catch (err: any) {
      console.error('加载产品列表失败:', err)
    } finally {
      setLoadingProducts(false)
    }
  }

  useEffect(() => {
    loadProducts()
  }, [])

  const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files
    if (files) {
      const newFiles: UploadedFile[] = Array.from(files).map((file) => ({
        id: Math.random().toString(36).substr(2, 9),
        name: file.name,
        size: file.size,
        type: file.type,
      }))
      setUploadedFiles([...uploadedFiles, ...newFiles])
    }
  }

  const handleRemoveFile = (fileId: string) => {
    setUploadedFiles(uploadedFiles.filter((file) => file.id !== fileId))
  }

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return "0 Bytes"
    const k = 1024
    const sizes = ["Bytes", "KB", "MB", "GB"]
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + " " + sizes[i]
  }

  async function onSubmit(values: z.infer<typeof formSchema>) {
    setIsSubmitting(true)
    try {
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8000'
      const response = await fetch(`${apiUrl}/api/achievements/pre-register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: values.name,
          version: values.version,
          product_id: values.productId,
          organization_name: values.organizationName,
          department_name: values.departmentName,
          has_baseline: values.hasBaseline,
          requirement_proposer: values.requirementProposer,
          achievement_form: values.achievementForm,
          sale_type: values.saleType,
          application_scenario: values.applicationScenario,
          type: values.type,
          description: values.description,
          owner: values.owner,
          achievement_target: values.achievementTarget,
          planned_acceptance_date: values.plannedAcceptanceDate || null,
          acceptance_method: values.acceptanceMethod,
          acceptor: values.acceptor,
          related_project_name: values.relatedProjectName,
          related_order_id: values.relatedOrderId,
          related_order_name: values.relatedOrderName,
          acceptance_requirements: values.acceptanceRequirements,
        }),
      })
      if (!response.ok) {
        const error = await response.json().catch(() => ({ detail: '预注册失败' }))
        throw new Error(error.detail || '预注册失败')
      }
      alert('成果预注册成功')
      router.push("/achievement")
    } catch (error: any) {
      console.error("注册失败:", error)
      alert('注册失败: ' + (error.message || '未知错误'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="container mx-auto py-6 max-w-5xl">
      <Card>
        <CardHeader>
          <CardTitle className="text-[22px] font-bold">成果预注册</CardTitle>
          <CardDescription>在项目立项制订计划的过程中，明确本周期的产出物，在此进行录入</CardDescription>
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              
              <Collapsible open={basicDataOpen} onOpenChange={setBasicDataOpen}>
                <CollapsibleTrigger className="flex items-center justify-between w-full p-3 bg-slate-50 rounded-lg hover:bg-slate-100 transition-colors">
                  <span className="font-semibold text-[15px]">基础数据</span>
                  {basicDataOpen ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                </CollapsibleTrigger>
                <CollapsibleContent className="pt-4">
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <FormField
                      control={form.control}
                      name="name"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="text-[13px]">
                            成果名称 <span className="text-red-500">*</span>
                          </FormLabel>
                          <FormControl>
                            <Input placeholder="请输入成果名称" className="h-8 text-[13px]" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={form.control}
                      name="productId"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="text-[13px]">
                            所属产品 <span className="text-red-500">*</span>
                          </FormLabel>
                          <Select onValueChange={field.onChange} value={field.value} disabled={loadingProducts}>
                            <FormControl>
                              <SelectTrigger className="h-8 text-[13px]">
                                {loadingProducts ? (
                                  <div className="flex items-center">
                                    <Loader2 className="h-3 w-3 animate-spin mr-2" />
                                    <span>加载中...</span>
                                  </div>
                                ) : (
                                  <SelectValue placeholder="请选择所属产品" />
                                )}
                              </SelectTrigger>
                            </FormControl>
                            <SelectContent>
                              {products.map((product) => (
                                <SelectItem key={product.id} value={product.id}>
                                  {product.name}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={form.control}
                      name="type"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="text-[13px]">
                            成果类型 <span className="text-red-500">*</span>
                          </FormLabel>
                          <Select onValueChange={field.onChange} value={field.value}>
                            <FormControl>
                              <SelectTrigger className="h-8 text-[13px]">
                                <SelectValue placeholder="请选择成果类型" />
                              </SelectTrigger>
                            </FormControl>
                            <SelectContent>
                              <SelectItem value="模块">模块</SelectItem>
                              <SelectItem value="功能">功能</SelectItem>
                              <SelectItem value="资产">资产</SelectItem>
                              <SelectItem value="文档">文档</SelectItem>
                              <SelectItem value="其他">其他</SelectItem>
                            </SelectContent>
                          </Select>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={form.control}
                      name="owner"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="text-[13px]">
                            负责人 <span className="text-red-500">*</span>
                          </FormLabel>
                          <FormControl>
                            <Input placeholder="请输入负责人" className="h-8 text-[13px]" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={form.control}
                      name="version"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="text-[13px]">
                            成果版本 <span className="text-red-500">*</span>
                          </FormLabel>
                          <FormControl>
                            <Input placeholder="例如：V1.0.0" className="h-8 text-[13px]" {...field} />
                          </FormControl>
                          <FormDescription className="text-[11px]">格式：V+版本号</FormDescription>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={form.control}
                      name="organizationName"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="text-[13px]">
                            所属机构 <span className="text-red-500">*</span>
                          </FormLabel>
                          <FormControl>
                            <Input placeholder="请输入所属机构" className="h-8 text-[13px]" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={form.control}
                      name="departmentName"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="text-[13px]">
                            所属部门 <span className="text-red-500">*</span>
                          </FormLabel>
                          <FormControl>
                            <Input placeholder="请输入所属部门" className="h-8 text-[13px]" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={form.control}
                      name="hasBaseline"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="text-[13px]">
                            是否有基线 <span className="text-red-500">*</span>
                          </FormLabel>
                          <Select onValueChange={field.onChange} value={field.value}>
                            <FormControl>
                              <SelectTrigger className="h-8 text-[13px]">
                                <SelectValue placeholder="请选择" />
                              </SelectTrigger>
                            </FormControl>
                            <SelectContent>
                              <SelectItem value="有基线">有基线</SelectItem>
                              <SelectItem value="无基线">无基线</SelectItem>
                            </SelectContent>
                          </Select>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={form.control}
                      name="requirementProposer"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="text-[13px]">
                            成果需求提出人 <span className="text-red-500">*</span>
                          </FormLabel>
                          <FormControl>
                            <Input placeholder="请输入需求提出人" className="h-8 text-[13px]" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={form.control}
                      name="achievementForm"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="text-[13px]">
                            成果形态 <span className="text-red-500">*</span>
                          </FormLabel>
                          <Select onValueChange={field.onChange} value={field.value}>
                            <FormControl>
                              <SelectTrigger className="h-8 text-[13px]">
                                <SelectValue placeholder="请选择成果形态" />
                              </SelectTrigger>
                            </FormControl>
                            <SelectContent>
                              <SelectItem value="软件">软件</SelectItem>
                              <SelectItem value="硬件">硬件</SelectItem>
                              <SelectItem value="文档">文档</SelectItem>
                              <SelectItem value="服务">服务</SelectItem>
                              <SelectItem value="其他">其他</SelectItem>
                            </SelectContent>
                          </Select>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={form.control}
                      name="saleType"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="text-[13px]">
                            成果可售类型 <span className="text-red-500">*</span>
                          </FormLabel>
                          <Select onValueChange={field.onChange} value={field.value}>
                            <FormControl>
                              <SelectTrigger className="h-8 text-[13px]">
                                <SelectValue placeholder="请选择可售类型" />
                              </SelectTrigger>
                            </FormControl>
                            <SelectContent>
                              <SelectItem value="可售">可售</SelectItem>
                              <SelectItem value="不可售">不可售</SelectItem>
                              <SelectItem value="内部使用">内部使用</SelectItem>
                            </SelectContent>
                          </Select>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>

                  <div className="mt-4">
                    <FormField
                      control={form.control}
                      name="description"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="text-[13px]">
                            成果描述 <span className="text-red-500">*</span>
                          </FormLabel>
                          <FormControl>
                            <Textarea placeholder="请输入成果描述，详细描述成果的核心用途及解决的问题" className="min-h-[80px] text-[13px]" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>

                  <div className="mt-4">
                    <FormField
                      control={form.control}
                      name="applicationScenario"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="text-[13px]">
                            应用场景 <span className="text-red-500">*</span>
                          </FormLabel>
                          <FormControl>
                            <Textarea placeholder="请输入应用场景描述，多个场景用分号隔开" className="min-h-[60px] text-[13px]" {...field} />
                          </FormControl>
                          <FormDescription className="text-[11px]">多个应用场景用分号（；）隔开</FormDescription>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>
                </CollapsibleContent>
              </Collapsible>

              <Collapsible open={planDataOpen} onOpenChange={setPlanDataOpen}>
                <CollapsibleTrigger className="flex items-center justify-between w-full p-3 bg-slate-50 rounded-lg hover:bg-slate-100 transition-colors">
                  <span className="font-semibold text-[15px]">计划数据</span>
                  {planDataOpen ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                </CollapsibleTrigger>
                <CollapsibleContent className="pt-4">
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <FormField
                      control={form.control}
                      name="plannedAcceptanceDate"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="text-[13px]">
                            计划验收日期 <span className="text-red-500">*</span>
                          </FormLabel>
                          <FormControl>
                            <Input type="date" className="h-8 text-[13px]" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={form.control}
                      name="acceptor"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="text-[13px]">
                            成果验收人 <span className="text-red-500">*</span>
                          </FormLabel>
                          <FormControl>
                            <Input placeholder="请输入验收人，多个用分号隔开" className="h-8 text-[13px]" {...field} />
                          </FormControl>
                          <FormDescription className="text-[11px]">多个验收人用分号隔开</FormDescription>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={form.control}
                      name="acceptanceMethod"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="text-[13px]">
                            验收方式 <span className="text-red-500">*</span>
                          </FormLabel>
                          <Select onValueChange={field.onChange} value={field.value}>
                            <FormControl>
                              <SelectTrigger className="h-8 text-[13px]">
                                <SelectValue placeholder="请选择验收方式" />
                              </SelectTrigger>
                            </FormControl>
                            <SelectContent>
                              <SelectItem value="系统演示">系统演示</SelectItem>
                              <SelectItem value="方案试讲">方案试讲</SelectItem>
                              <SelectItem value="POC评分">POC评分</SelectItem>
                              <SelectItem value="其它">其它</SelectItem>
                            </SelectContent>
                          </Select>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>

                  <div className="mt-4">
                    <FormField
                      control={form.control}
                      name="achievementTarget"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="text-[13px]">
                            成果目标 <span className="text-red-500">*</span>
                          </FormLabel>
                          <FormControl>
                            <Textarea placeholder="请输入成果目标描述" className="min-h-[60px] text-[13px]" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>

                  <div className="mt-4">
                    <FormField
                      control={form.control}
                      name="acceptanceRequirements"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="text-[13px]">
                            验收要求 <span className="text-red-500">*</span>
                          </FormLabel>
                          <FormControl>
                            <Textarea placeholder="请输入验收要求，明确成果的验收标准和要求" className="min-h-[80px] text-[13px]" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>
                </CollapsibleContent>
              </Collapsible>

              <Collapsible open={execPlanDataOpen} onOpenChange={setExecPlanDataOpen}>
                <CollapsibleTrigger className="flex items-center justify-between w-full p-3 bg-slate-50 rounded-lg hover:bg-slate-100 transition-colors">
                  <span className="font-semibold text-[15px]">执行计划类参数</span>
                  {execPlanDataOpen ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                </CollapsibleTrigger>
                <CollapsibleContent className="pt-4">
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <FormField
                      control={form.control}
                      name="relatedProjectName"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="text-[13px]">关联项目</FormLabel>
                          <FormControl>
                            <Input placeholder="请输入关联项目名称" className="h-8 text-[13px]" {...field} />
                          </FormControl>
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={form.control}
                      name="relatedOrderId"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="text-[13px]">关联订单编号</FormLabel>
                          <FormControl>
                            <Input placeholder="请输入关联订单编号" className="h-8 text-[13px]" {...field} />
                          </FormControl>
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={form.control}
                      name="relatedOrderName"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="text-[13px]">关联订单名称</FormLabel>
                          <FormControl>
                            <Input placeholder="请输入关联订单名称" className="h-8 text-[13px]" {...field} />
                          </FormControl>
                        </FormItem>
                      )}
                    />
                  </div>
                </CollapsibleContent>
              </Collapsible>

              <div>
                <FormLabel className="text-[13px]">附件上传</FormLabel>
                <FormDescription className="text-[11px]">上传相关的附件文件</FormDescription>
                <div className="mt-2">
                  <div className="flex items-center justify-center w-full">
                    <label
                      htmlFor="file-upload"
                      className="flex flex-col items-center justify-center w-full h-24 border-2 border-dashed border-gray-300 rounded-lg cursor-pointer bg-gray-50 hover:bg-gray-100"
                    >
                      <div className="flex flex-col items-center justify-center pt-5 pb-6">
                        <Upload className="w-6 h-6 mb-1 text-gray-400" />
                        <p className="mb-1 text-[12px] text-gray-500">
                          <span className="font-semibold">点击上传</span> 或拖拽文件到此处
                        </p>
                        <p className="text-[11px] text-gray-500">支持 PDF, DOC, DOCX, ZIP 等格式</p>
                      </div>
                      <input id="file-upload" type="file" className="hidden" multiple onChange={handleFileUpload} />
                    </label>
                  </div>
                  {uploadedFiles.length > 0 && (
                    <div className="mt-3 space-y-2">
                      {uploadedFiles.map((file) => (
                        <div
                          key={file.id}
                          className="flex items-center justify-between p-2 bg-gray-50 rounded-lg border text-[13px]"
                        >
                          <div className="flex items-center space-x-3">
                            <div className="flex-1 min-w-0">
                              <p className="text-[13px] font-medium text-gray-900 truncate">{file.name}</p>
                              <p className="text-[11px] text-gray-500">{formatFileSize(file.size)}</p>
                            </div>
                          </div>
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            className="h-6 w-6 p-0"
                            onClick={() => handleRemoveFile(file.id)}
                          >
                            <X className="h-3 w-3" />
                          </Button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>

              <div className="flex justify-end space-x-4 pt-4">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => router.push("/achievement")}
                  disabled={isSubmitting}
                >
                  取消
                </Button>
                <Button type="submit" size="sm" disabled={isSubmitting}>
                  {isSubmitting && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                  {isSubmitting ? "提交中..." : "提交预注册"}
                </Button>
              </div>
            </form>
          </Form>
        </CardContent>
      </Card>
    </div>
  )
}
