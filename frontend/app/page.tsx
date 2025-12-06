import Navbar from "@/src/components/Navbar";
import HeroSection from "@/src/components/HeroSection"


export default function Home() {
  return (
    <div className="min-h-screen">
      <Navbar />
        <main>
          <HeroSection />
        </main>
    </div>
  )
}
