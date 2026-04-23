"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { Trophy, Plus, Search, ArrowUpDown, CheckCircle2, Clock, FileText, Download, Package, Layers3, Building2, Cog, Loader2, Archive, RotateCcw, Trash2, ChevronLeft, ChevronRight } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Checkbox } from "@/components/ui/checkbox"
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Textarea } from "@/components/ui/textarea"
import type { Achievement, AchievementStatus, AchievementType } from "@/lib/types"

const statusColors: Record<AchievementStatus, string> = {
  "预注册": "bg-yellow-100 text-yellow-800",
  "注册": "bg-blue-100 text-blue-800",
  "登记": "bg-green-100 text-green-800",
  "下架": "bg-gray-100 text-gray-600",
  "已删除": "bg-red-50 text-red-500",
}

const statusIcons: Record<AchievementStatus, React.ReactNode> = {
  "预注册": <Clock className="h-3 w-3" />,
  "注册": <CheckCircle2 className="h-3 w-3" />,
  "登记": <CheckCircle2 className="h-3 w-3" />,
  "下架": <Archive className="h-3 w-3" />,
  "已删除": <Trash2 className="h-3 w-3" />,
}

const statusMap: Record<string, string> = {
  "预注册": "pre_register",
  "注册": "registered",
  "登记": "recorded",
  "下架": "offline",
  "已删除": "deleted",
}

const statusReverseMap: Record<string, string> = {
  "pre_register": "预注册",
  "registered": "注册",
  "register": "注册",
  "recorded": "登记",
  "record": "登记",
  "offline": "下架",
  "deleted": "已删除",
}

export default function AchievementPage() {
  const router = useRouter()
  const [searchTerm, setSearchTerm] = useState("")
  const [selectedStatus, setSelectedStatus] = useState<string>("all")
  const [selectedType, setSelectedType] = useState<string>("all")
  const [selectedProduct, setSelectedProduct] = useState<string>("all")
  const [selectedAchievements, setSelectedAchievements] = useState<Set<string>>(new Set())
  const [achievements, setAchievements] = useState<Achievement[]>([])
  const [products, setProducts] = useState<any[]>([])
  const [statistics, setStatistics] = useState<any>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(1)
  const [total, setTotal] = useState(0)
  const pageSize = 20
  const [offlineDialogOpen, setOfflineDialogOpen] = useState(false)
  const [onlineDialogOpen, setOnlineDialogOpen] = useState(false)
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [selectedAchievementId, setSelectedAchievementId] = useState<string | null>(null)
  const [selectedAchievementName, setSelectedAchievementName] = useState<string>("")
  const [changeReason, setChangeReason] = useState("")
  const [actionLoading, setActionLoading] = useState(false)

  const loadAchievements = async () => {
    console.log('[成果管理] 开始加载成果列表...')
    try {
      setLoading(true)
      setError(null)
      const params: any = {
        page,
        page_size: pageSize,
      }
      if (selectedStatus !== "all") params.status = statusMap[selectedStatus] || selectedStatus
      if (selectedType !== "all") params.type = selectedType
      if (selectedProduct !== "all") params.product_id = selectedProduct
      if (searchTerm) params.keyword = searchTerm

      console.log('[成果管理] 请求参数:', params)
      
      // 直接使用fetch，绕过api.ts
      const queryParams = new URLSearchParams()
      if (params.page) queryParams.append('page', params.page.toString())
      if (params.page_size) queryParams.append('page_size', params.page_size.toString())
      if (params.status) queryParams.append('status', params.status)
      if (params.type) queryParams.append('type', params.type)
      if (params.product_id) queryParams.append('product_id', params.product_id)
      if (params.keyword) queryParams.append('keyword', params.keyword)
      
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8000'
      const url = `${apiUrl}/api/achievements?${queryParams.toString()}`
      console.log('[成果管理] 请求URL:', url)
      
      const response = await fetch(url, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      })
      
      console.log('[成果管理] 响应状态:', response.status)
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }
      
      const data = await response.json()
      console.log('[成果管理] 响应数据:', data)
      
      setAchievements(data.items.map((item: any) => ({
        id: item.id,
        name: item.name,
        version: item.version,
        productVersion: item.product_version || item.version,
        versionStatus: item.version_status || '正常',
        versionType: item.version_type || '当期创建',
        changeDescription: item.change_description || '',
        delayReason: item.delay_reason || '',
        acceptanceResult: item.acceptance_result || '',
        productId: item.product_id,
        productName: item.product_name || '',
        moduleId: item.module_id,
        moduleName: item.module_name || '',
        type: item.type || '其他',
        description: item.description || '',
        status: statusReverseMap[item.status] || item.status,
        owner: item.owner || '',
        preRegisterTime: item.pre_register_time ? new Date(item.pre_register_time).toISOString().split('T')[0] : '',
        registerTime: item.register_time ? new Date(item.register_time).toISOString().split('T')[0] : null,
        recordTime: item.record_time ? new Date(item.record_time).toISOString().split('T')[0] : null,
        relatedOrderId: item.related_order_id || '',
        relatedProjectId: item.related_project_id || '',
        acceptanceRequirements: item.acceptance_requirements || '',
        attachments: [],
        checkoutHistory: [],
        versionHistory: [],
        createdAt: new Date(item.created_at).toISOString().split('T')[0],
        updatedAt: new Date(item.updated_at).toISOString().split('T')[0],
      })))
      setTotal(data.total)
    } catch (err: any) {
      setError(err.message || '加载失败')
      console.error('加载成果列表失败:', err)
    } finally {
      setLoading(false)
    }
  }

  const loadProducts = async () => {
    try {
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8000'
      const response = await fetch(`${apiUrl}/api/products?page_size=100`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      })
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }
      const data = await response.json()
      setProducts(data.items)
    } catch (err: any) {
      console.error('加载产品列表失败:', err)
    }
  }

  const loadStatistics = async () => {
    try {
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8000'
      const response = await fetch(`${apiUrl}/api/achievements/statistics`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      })
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }
      const stats = await response.json()
      setStatistics(stats)
    } catch (err: any) {
      console.error('加载统计数据失败:', err)
    }
  }

  useEffect(() => {
    setPage(1)
    loadAchievements()
  }, [selectedStatus, selectedType, selectedProduct])

  useEffect(() => {
    loadAchievements()
  }, [page])

  useEffect(() => {
    const timer = setTimeout(() => {
      if (searchTerm !== undefined) {
        setPage(1)
        loadAchievements()
      }
    }, 500)
    return () => clearTimeout(timer)
  }, [searchTerm])

  useEffect(() => {
    loadProducts()
    loadStatistics()
  }, [])

  const stats = {
    totalCount: statistics?.total_count || 0,
    preRegisterCount: statistics?.pre_register_count || 0,
    registerCount: statistics?.register_count || 0,
    recordCount: statistics?.record_count || 0,
    offlineCount: statistics?.offline_count || 0,
    checkoutCount: statistics?.checkout_count || 0,
    byType: {
      模块: statistics?.by_type?.['模块'] || 0,
      功能: statistics?.by_type?.['功能'] || 0,
      资产: statistics?.by_type?.['资产'] || 0,
      文档: statistics?.by_type?.['文档'] || 0,
      其他: statistics?.by_type?.['其他'] || 0,
    },
  }

  const handleOffline = (achievementId: string) => {
    setSelectedAchievementId(achievementId)
    setChangeReason("")
    setOfflineDialogOpen(true)
  }

  const handleOnline = (achievementId: string) => {
    setSelectedAchievementId(achievementId)
    setChangeReason("")
    setOnlineDialogOpen(true)
  }

  const confirmOffline = async () => {
    if (!selectedAchievementId) return
    setActionLoading(true)
    try {
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8000'
      const token = localStorage.getItem('token')
      const response = await fetch(`${apiUrl}/api/achievements/${encodeURIComponent(selectedAchievementId)}/offline`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({ reason: changeReason }),
      })
      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.detail || '下架失败')
      }
      setOfflineDialogOpen(false)
      loadAchievements()
      loadStatistics()
    } catch (err: any) {
      setError(err.message || '下架失败')
    } finally {
      setActionLoading(false)
    }
  }

  const confirmOnline = async () => {
    if (!selectedAchievementId) return
    setActionLoading(true)
    try {
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8000'
      const token = localStorage.getItem('token')
      const response = await fetch(`${apiUrl}/api/achievements/${encodeURIComponent(selectedAchievementId)}/online`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({ reason: changeReason }),
      })
      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.detail || '上架失败')
      }
      setOnlineDialogOpen(false)
      loadAchievements()
      loadStatistics()
    } catch (err: any) {
      setError(err.message || '上架失败')
    } finally {
      setActionLoading(false)
    }
  }

  const handleDelete = (achievementId: string, achievementName: string) => {
    setSelectedAchievementId(achievementId)
    setSelectedAchievementName(achievementName)
    setDeleteDialogOpen(true)
  }

  const confirmDelete = async () => {
    if (!selectedAchievementId) return
    setActionLoading(true)
    try {
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8000'
      console.log('[删除成果] 请求URL:', `${apiUrl}/api/achievements/${encodeURIComponent(selectedAchievementId)}/delete`)
      const response = await fetch(`${apiUrl}/api/achievements/${encodeURIComponent(selectedAchievementId)}/delete`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
      })
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ detail: '删除失败' }))
        throw new Error(errorData.detail || '删除失败')
      }
      setDeleteDialogOpen(false)
      loadAchievements()
      loadStatistics()
      alert('删除成功')
    } catch (err: any) {
      console.error('[删除成果] 错误:', err)
      const errorMsg = err.message === 'Failed to fetch' 
        ? '无法连接到后端服务，请确保后端服务已启动' 
        : (err.message || '删除失败')
      setError(errorMsg)
      alert(errorMsg)
    } finally {
      setActionLoading(false)
    }
  }

  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelectedAchievements(new Set(achievements.map((a) => a.id)))
    } else {
      setSelectedAchievements(new Set())
    }
  }

  const handleSelectAchievement = (id: string, checked: boolean) => {
    const newSelected = new Set(selectedAchievements)
    if (checked) {
      newSelected.add(id)
    } else {
      newSelected.delete(id)
    }
    setSelectedAchievements(newSelected)
  }

  const handleBatchCheckout = () => {
    console.log("批量出库:", Array.from(selectedAchievements))
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-2">
        <div>
          <h1 className="text-lg font-bold tracking-tight">成果管理</h1>
          <p className="text-muted-foreground text-xs">企业成果资产的全生命周期管理</p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm" className="h-7 text-xs" asChild>
            <Link href="/achievement/pre-register">
              <Plus className="h-3 w-3 mr-1" />
              成果预注册
            </Link>
          </Button>
        </div>
      </div>

      <div className="grid gap-2 md:grid-cols-5">
        <Card className="p-0">
          <CardContent className="p-3">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs text-muted-foreground">总成果</p>
                <p className="text-xl font-bold">{stats.totalCount}</p>
              </div>
              <Trophy className="h-4 w-4 text-muted-foreground" />
            </div>
          </CardContent>
        </Card>

        <Card className="p-0">
          <CardContent className="p-3">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs text-muted-foreground">预注册</p>
                <p className="text-xl font-bold">{stats.preRegisterCount}</p>
              </div>
              <Clock className="h-4 w-4 text-muted-foreground" />
            </div>
          </CardContent>
        </Card>

        <Card className="p-0">
          <CardContent className="p-3">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs text-muted-foreground">已登记</p>
                <p className="text-xl font-bold">{stats.recordCount}</p>
              </div>
              <CheckCircle2 className="h-4 w-4 text-muted-foreground" />
            </div>
          </CardContent>
        </Card>

        <Card className="p-0">
          <CardContent className="p-3">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs text-muted-foreground">已下架</p>
                <p className="text-xl font-bold">{stats.offlineCount}</p>
              </div>
              <Archive className="h-4 w-4 text-muted-foreground" />
            </div>
          </CardContent>
        </Card>

        <Card className="p-0">
          <CardContent className="p-3">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs text-muted-foreground">出库次数</p>
                <p className="text-xl font-bold">{stats.checkoutCount}</p>
              </div>
              <Download className="h-4 w-4 text-muted-foreground" />
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader className="p-3">
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-2">
            <div>
              <CardTitle className="text-sm">成果列表</CardTitle>
            </div>
            <div className="flex items-center gap-2 justify-end flex-wrap">
              <Select value={selectedProduct} onValueChange={setSelectedProduct}>
                <SelectTrigger className="w-[120px] h-7 text-xs">
                  <SelectValue placeholder="选择产品" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">全部产品</SelectItem>
                  {products.map((product) => (
                    <SelectItem key={product.id} value={product.id}>
                      {product.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Select value={selectedStatus} onValueChange={setSelectedStatus}>
                <SelectTrigger className="w-[100px] h-7 text-xs">
                  <SelectValue placeholder="选择状态" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">全部状态</SelectItem>
                  <SelectItem value="预注册">预注册</SelectItem>
                  <SelectItem value="注册">注册</SelectItem>
                  <SelectItem value="登记">登记</SelectItem>
                  <SelectItem value="下架">下架</SelectItem>
                  <SelectItem value="已删除">已删除</SelectItem>
                </SelectContent>
              </Select>
              <Select value={selectedType} onValueChange={setSelectedType}>
                <SelectTrigger className="w-[100px] h-7 text-xs">
                  <SelectValue placeholder="选择类型" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">全部类型</SelectItem>
                  <SelectItem value="模块">模块</SelectItem>
                  <SelectItem value="功能">功能</SelectItem>
                  <SelectItem value="资产">资产</SelectItem>
                  <SelectItem value="文档">文档</SelectItem>
                  <SelectItem value="其他">其他</SelectItem>
                </SelectContent>
              </Select>
              <Input
                placeholder="搜索成果..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-[150px] h-7 text-xs"
              />
            </div>
          </div>
          {selectedAchievements.size > 0 && (
            <div className="flex items-center gap-2 mt-2">
              <span className="text-xs text-muted-foreground">已选择 {selectedAchievements.size} 项</span>
              <Button variant="outline" size="sm" className="h-6 text-xs" onClick={handleBatchCheckout}>
                <Download className="h-3 w-3 mr-1" />
                批量出库
              </Button>
            </div>
          )}
        </CardHeader>
        <CardContent>
          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-red-800 text-sm">
              {error}
            </div>
          )}
          <div className="rounded-md border overflow-x-auto">
            <TooltipProvider>
            <Table className="compact-table">
              <TableHeader>
                <TableRow className="bg-muted/50">
                  <TableHead className="w-[40px] px-2 py-2">
                    <Checkbox
                      checked={selectedAchievements.size === achievements.length && achievements.length > 0}
                      onCheckedChange={handleSelectAll}
                    />
                  </TableHead>
                  <TableHead className="px-2 py-2 whitespace-nowrap text-xs">成果ID</TableHead>
                  <TableHead className="px-2 py-2 whitespace-nowrap text-xs">成果名称</TableHead>
                  <TableHead className="px-2 py-2 whitespace-nowrap text-xs">产品版本</TableHead>
                  <TableHead className="px-2 py-2 whitespace-nowrap text-xs">版本状态</TableHead>
                  <TableHead className="px-2 py-2 whitespace-nowrap text-xs">版本类型</TableHead>
                  <TableHead className="px-2 py-2 whitespace-nowrap text-xs">所属产品</TableHead>
                  <TableHead className="px-2 py-2 whitespace-nowrap text-xs">所属模块</TableHead>
                  <TableHead className="px-2 py-2 whitespace-nowrap text-xs">成果类型</TableHead>
                  <TableHead className="px-2 py-2 whitespace-nowrap text-xs">状态</TableHead>
                  <TableHead className="px-2 py-2 whitespace-nowrap text-xs">负责人</TableHead>
                  <TableHead className="px-2 py-2 whitespace-nowrap text-xs">预注册时间</TableHead>
                  <TableHead className="px-2 py-2 whitespace-nowrap text-xs">注册时间</TableHead>
                  <TableHead className="px-2 py-2 whitespace-nowrap text-xs">登记时间</TableHead>
                  <TableHead className="px-2 py-2 whitespace-nowrap text-xs">出库</TableHead>
                  <TableHead className="sticky right-0 bg-muted/50 px-2 py-2 text-xs min-w-[120px] shadow-[-4px_0_4px_rgba(0,0,0,0.1)]">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={17} className="text-center py-8">
                      <div className="flex items-center justify-center">
                        <Loader2 className="h-6 w-6 animate-spin mr-2" />
                        加载中...
                      </div>
                    </TableCell>
                  </TableRow>
                ) : achievements.length > 0 ? (
                  achievements.map((achievement) => (
                    <TableRow key={achievement.id} className={`h-9 ${achievement.status === '下架' ? 'bg-gray-50' : achievement.status === '已删除' ? 'bg-red-50 opacity-60' : ''}`}>
                      <TableCell className="px-2 py-1">
                        <Checkbox
                          checked={selectedAchievements.has(achievement.id)}
                          onCheckedChange={(checked) => handleSelectAchievement(achievement.id, checked as boolean)}
                        />
                      </TableCell>
                      <TableCell className="font-medium px-2 py-1 text-xs whitespace-nowrap">{achievement.id}</TableCell>
                      <TableCell className="px-2 py-1 text-xs max-w-[150px] truncate">
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <span className="cursor-default">{achievement.name}</span>
                          </TooltipTrigger>
                          <TooltipContent side="top" className="max-w-[300px]">
                            <p>{achievement.name}</p>
                          </TooltipContent>
                        </Tooltip>
                      </TableCell>
                      <TableCell className="px-2 py-1 text-xs">{achievement.productVersion}</TableCell>
                      <TableCell className="px-2 py-1">
                        <Badge variant={achievement.versionStatus === '正常' ? 'default' : 'secondary'} className="text-[10px] h-5">
                          {achievement.versionStatus}
                        </Badge>
                      </TableCell>
                      <TableCell className="px-2 py-1 text-xs">{achievement.versionType}</TableCell>
                      <TableCell className="px-2 py-1 text-xs max-w-[100px] truncate">
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <span className="cursor-default">{achievement.productName}</span>
                          </TooltipTrigger>
                          <TooltipContent side="top" className="max-w-[300px]">
                            <p>{achievement.productName}</p>
                          </TooltipContent>
                        </Tooltip>
                      </TableCell>
                      <TableCell className="px-2 py-1 text-xs max-w-[100px] truncate">
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <span className="cursor-default">{achievement.moduleName}</span>
                          </TooltipTrigger>
                          <TooltipContent side="top" className="max-w-[300px]">
                            <p>{achievement.moduleName}</p>
                          </TooltipContent>
                        </Tooltip>
                      </TableCell>
                      <TableCell className="px-2 py-1 text-xs">{achievement.type}</TableCell>
                      <TableCell className="px-2 py-1 whitespace-nowrap">
                        <Badge className={`${statusColors[achievement.status]} text-[10px] h-5 whitespace-nowrap`}>
                          {achievement.status}
                        </Badge>
                      </TableCell>
                      <TableCell className="px-2 py-1 text-xs">{achievement.owner}</TableCell>
                      <TableCell className="px-2 py-1 text-xs whitespace-nowrap">{achievement.preRegisterTime}</TableCell>
                      <TableCell className="px-2 py-1 text-xs whitespace-nowrap">{achievement.registerTime || "-"}</TableCell>
                      <TableCell className="px-2 py-1 text-xs whitespace-nowrap">{achievement.recordTime || "-"}</TableCell>
                      <TableCell className="px-2 py-1 text-xs text-center">{achievement.checkoutHistory.length}</TableCell>
                      <TableCell className="sticky right-0 bg-white px-2 py-1 shadow-[-4px_0_4px_rgba(0,0,0,0.05)]">
                        <div className="flex items-center gap-1">
                          {achievement.status === '已删除' ? (
                            <Button variant="ghost" size="sm" className="h-6 text-xs px-2" asChild>
                              <Link href={`/achievement/${encodeURIComponent(achievement.id)}`}>详情</Link>
                            </Button>
                          ) : achievement.status === '下架' ? (
                            <Button 
                              variant="outline" 
                              size="sm" 
                              onClick={() => handleOnline(achievement.id)}
                              className="h-6 text-xs px-2 text-green-600 hover:text-green-700"
                            >
                              <RotateCcw className="h-3 w-3 mr-1" />
                              上架
                            </Button>
                          ) : achievement.status === '预注册' ? (
                            <Button 
                              variant="outline" 
                              size="sm" 
                              onClick={() => handleDelete(achievement.id, achievement.name)}
                              className="h-6 text-xs px-2 text-red-600 hover:text-red-700"
                            >
                              <Trash2 className="h-3 w-3 mr-1" />
                              删除
                            </Button>
                          ) : achievement.status === '登记' ? (
                            <Button 
                              variant="outline" 
                              size="sm" 
                              onClick={() => handleOffline(achievement.id)}
                              className="h-6 text-xs px-2 text-gray-600 hover:text-gray-700"
                            >
                              <Archive className="h-3 w-3 mr-1" />
                              下架
                            </Button>
                          ) : (
                            <Button variant="outline" size="sm" className="h-6 text-xs px-2" asChild>
                              <Link href={`/achievement/change?achievement_id=${encodeURIComponent(achievement.id)}`}>变更</Link>
                            </Button>
                          )}
                          {achievement.status !== '已删除' && (
                            <Button variant="ghost" size="sm" className="h-6 text-xs px-2" asChild>
                              <Link href={`/achievement/${encodeURIComponent(achievement.id)}`}>详情</Link>
                            </Button>
                          )}
                        </div>
                      </TableCell>
                    </TableRow>
                  ))
                ) : (
                  <TableRow>
                    <TableCell colSpan={17} className="text-center py-4">
                      未找到匹配的成果
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
            </TooltipProvider>
          </div>
          
          {total > pageSize && (
            <div className="flex items-center justify-between px-2 py-3 border-t">
              <div className="text-xs text-muted-foreground">
                共 {total} 条记录，第 {page} / {Math.ceil(total / pageSize)} 页
              </div>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  className="h-7 text-xs"
                  onClick={() => setPage(p => Math.max(1, p - 1))}
                  disabled={page <= 1}
                >
                  <ChevronLeft className="h-3 w-3 mr-1" />
                  上一页
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  className="h-7 text-xs"
                  onClick={() => setPage(p => Math.min(Math.ceil(total / pageSize), p + 1))}
                  disabled={page >= Math.ceil(total / pageSize)}
                >
                  下一页
                  <ChevronRight className="h-3 w-3 ml-1" />
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      <AlertDialog open={offlineDialogOpen} onOpenChange={setOfflineDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认下架成果</AlertDialogTitle>
            <AlertDialogDescription>
              下架后该成果将不允许出库，您可以随时重新上架。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <div className="py-4">
            <label className="text-sm font-medium">下架原因（可选）</label>
            <Textarea
              value={changeReason}
              onChange={(e) => setChangeReason(e.target.value)}
              placeholder="请输入下架原因..."
              className="mt-2"
            />
          </div>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={actionLoading}>取消</AlertDialogCancel>
            <AlertDialogAction onClick={confirmOffline} disabled={actionLoading}>
              {actionLoading ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
              确认下架
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={onlineDialogOpen} onOpenChange={setOnlineDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认上架成果</AlertDialogTitle>
            <AlertDialogDescription>
              上架后该成果将恢复可出库状态。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <div className="py-4">
            <label className="text-sm font-medium">上架原因（可选）</label>
            <Textarea
              value={changeReason}
              onChange={(e) => setChangeReason(e.target.value)}
              placeholder="请输入上架原因..."
              className="mt-2"
            />
          </div>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={actionLoading}>取消</AlertDialogCancel>
            <AlertDialogAction onClick={confirmOnline} disabled={actionLoading}>
              {actionLoading ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
              确认上架
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认删除成果</AlertDialogTitle>
            <AlertDialogDescription>
              确定要删除成果「{selectedAchievementName}」吗？删除后将无法恢复，但记录仍会保留在系统中。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={actionLoading}>取消</AlertDialogCancel>
            <AlertDialogAction 
              onClick={confirmDelete} 
              disabled={actionLoading}
              className="bg-red-600 hover:bg-red-700"
            >
              {actionLoading ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
              确认删除
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
